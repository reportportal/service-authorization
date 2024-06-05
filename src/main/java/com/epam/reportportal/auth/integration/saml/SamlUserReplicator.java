/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.integration.saml;

import static com.epam.reportportal.auth.util.AuthUtils.CROP_DOMAIN;
import static com.epam.reportportal.auth.util.AuthUtils.NORMALIZE_STRING;

import com.epam.reportportal.auth.event.activity.AssignUserEvent;
import com.epam.reportportal.auth.event.activity.ProjectCreatedEvent;
import com.epam.reportportal.auth.event.activity.UserCreatedEvent;
import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.commons.ContentTypeResolver;
import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.util.PersonalProjectService;
import com.epam.ta.reportportal.ws.model.ErrorType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Replicates user from SAML response into database if it is not exist.
 *
 * @author Yevgeniy Svalukhin
 */
@Component
public class SamlUserReplicator extends AbstractUserReplicator {

  private final IntegrationTypeRepository integrationTypeRepository;
  private final IntegrationRepository integrationRepository;

  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public SamlUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
      PersonalProjectService personalProjectService, UserBinaryDataService userBinaryDataService,
      IntegrationTypeRepository integrationTypeRepository,
      IntegrationRepository integrationRepository, ContentTypeResolver contentTypeResolver,
      ApplicationEventPublisher eventPublisher) {
    super(userRepository, projectRepository, personalProjectService, userBinaryDataService,
        contentTypeResolver
    );
    this.integrationTypeRepository = integrationTypeRepository;
    this.integrationRepository = integrationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public User replicateUser(ReportPortalSamlAuthentication samlAuthentication) {
    String userName = CROP_DOMAIN.apply(samlAuthentication.getPrincipal());
    Optional<User> userOptional = userRepository.findByLogin(userName);

    if (userOptional.isPresent()) {
      return userOptional.get();
    }

    IntegrationType samlIntegrationType =
        integrationTypeRepository.findByName(AuthIntegrationType.SAML.getName()).orElseThrow(
            () -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND,
                AuthIntegrationType.SAML.getName()
            ));

    List<Integration> providers = integrationRepository.findAllGlobalByType(samlIntegrationType);

    Optional<Integration> samlProvider = providers.stream().filter(provider -> {
      Optional<String> idpUrlOptional = SamlParameter.IDP_URL.getParameter(provider);
      return idpUrlOptional.isPresent() && idpUrlOptional.get()
          .equalsIgnoreCase(samlAuthentication.getIssuer());
    }).findFirst();

    User user = new User();
    user.setLogin(userName);

    List<Attribute> details = samlAuthentication.getDetails();

    if (samlProvider.isPresent()) {
      populateUserDetailsIfSettingsArePresent(user, samlProvider.get(), details);
    } else {
      populateUserDetails(user, details);
    }

    user.setUserType(UserType.SAML);
    user.setExpired(false);

    Project project = generatePersonalProject(user);
    //TODO BUG IF PROJECT HAS NO USERS BECAUSE OF iterator().next() on empty collection
    user.getProjects().add(project.getUsers().iterator().next());

    user.setMetadata(defaultMetaData());

    userRepository.save(user);

    publishActivityEvents(user, project);

    return user;
  }

  private void publishActivityEvents(User user, Project project) {
    publishUserCreatedEvent(user);

    publishProjectCreatedEvent(project);

    publishUserAssignToProjectEvent(user, project);
  }

  private void publishUserCreatedEvent(User user) {
    UserCreatedEvent userCreatedEvent = new UserCreatedEvent(user.getId(), user.getLogin());
    eventPublisher.publishEvent(userCreatedEvent);
  }

  private void publishProjectCreatedEvent(Project project) {
    eventPublisher.publishEvent(new ProjectCreatedEvent(project.getId(), project.getName()));
  }

  private void publishUserAssignToProjectEvent(User user, Project project) {
    eventPublisher.publishEvent(
        new AssignUserEvent(user.getId(), user.getLogin(), project.getId()));
  }

  private void populateUserDetails(User user, List<Attribute> details) {
    String email = NORMALIZE_STRING.apply(
        findAttributeValue(details, UserAttribute.EMAIL.toString(), String.class));
    checkEmail(email);
    user.setEmail(email);

    String firstName =
        findAttributeValue(details, UserAttribute.FIRST_NAME.toString(), String.class);
    String lastName = findAttributeValue(details, UserAttribute.LAST_NAME.toString(), String.class);
    user.setFullName(String.join(" ", firstName, lastName));

    user.setRole(UserRole.USER);
  }

  private void populateUserDetailsIfSettingsArePresent(User user, Integration integration,
      List<Attribute> details) {
    String email = NORMALIZE_STRING.apply(findAttributeValue(details,
        SamlParameter.EMAIL_ATTRIBUTE.getParameter(integration).orElse(null), String.class
    ));
    checkEmail(email);
    user.setEmail(email);

    Optional<String> idpFullNameOptional =
        SamlParameter.FULL_NAME_ATTRIBUTE.getParameter(integration);

    if (idpFullNameOptional.isEmpty()) {
      String firstName = findAttributeValue(details,
          SamlParameter.FIRST_NAME_ATTRIBUTE.getParameter(integration).orElse(null), String.class
      );
      String lastName = findAttributeValue(details,
          SamlParameter.LAST_NAME_ATTRIBUTE.getParameter(integration).orElse(null), String.class
      );
      user.setFullName(String.join(" ", firstName, lastName));
    } else {
      String fullName = findAttributeValue(details, idpFullNameOptional.get(), String.class);
      user.setFullName(fullName);
    }

    String roles = findAttributeValue(details,
        SamlParameter.ROLES_ATTRIBUTE.getParameter(integration).orElse(null), String.class
    );
    if (Objects.nonNull(roles) && roles.toLowerCase().contains("admin")) {
      user.setRole(UserRole.ADMINISTRATOR);
    } else {
      user.setRole(UserRole.USER);
    }
  }

  private <T> T findAttributeValue(List<Attribute> attributes, String lookingFor, Class<T> castTo) {
    if (Objects.isNull(lookingFor) || CollectionUtils.isEmpty(attributes)) {
      return null;
    }

    Optional<Attribute> attribute =
        attributes.stream().filter(it -> it.getName().equalsIgnoreCase(lookingFor)).findFirst();

    if (attribute.isPresent()) {
      List<Object> values = attribute.get().getValues();
      if (!CollectionUtils.isEmpty(values)) {
        List<T> resultList = values.stream().filter(castTo::isInstance).map(castTo::cast)
            .collect(Collectors.toList());
        if (!resultList.isEmpty()) {
          return resultList.get(0);
        }
      }
    }
    return null;
  }
}
