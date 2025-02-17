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

import com.epam.reportportal.auth.binary.UserBinaryDataService;
import com.epam.reportportal.auth.commons.ContentTypeResolver;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.dao.ProjectRepository;
import com.epam.reportportal.auth.dao.UserRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.entity.project.Project;
import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.entity.user.UserRole;
import com.epam.reportportal.auth.entity.user.UserType;
import com.epam.reportportal.auth.event.activity.AssignUserEvent;
import com.epam.reportportal.auth.event.activity.ProjectCreatedEvent;
import com.epam.reportportal.auth.event.activity.UserCreatedEvent;
import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.model.saml.SamlResponse;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import com.epam.reportportal.auth.util.PersonalProjectService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replicates user from SAML response into database if it is not exist.
 *
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
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
  public User replicateUser(Saml2AuthenticationToken samlAuthentication) {
    SamlResponse samlResponse;
    try {
      samlResponse = SamlResponseParser.parseSamlResponse(
          samlAuthentication.getSaml2Response());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String userEmail = samlResponse.getNameId().value();
    Optional<User> userOptional = userRepository.findByEmail(userEmail);

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
      Optional<String> idpAliesOptional = SamlParameter.IDP_ALIAS.getParameter(provider);
      return idpAliesOptional.isPresent() && idpAliesOptional.get()
          .equalsIgnoreCase(samlResponse.getIssuer());
    }).findFirst();

    String userName = checkUserName(CROP_DOMAIN.apply(samlResponse.getNameId().value()));

    User user = new User();
    user.setLogin(userName);
    user.setUuid(UUID.randomUUID());
    user.setActive(Boolean.TRUE);

    if (samlProvider.isPresent()) {
      populateUserDetailsIfSettingsArePresent(user, samlProvider.get(),
          samlResponse.getAttributes());
    } else {
      populateUserDetails(user, samlResponse.getAttributes());
    }

    user.setUserType(UserType.SAML);
    user.setExpired(false);

    Project project = generatePersonalProject(user);
    user.getProjects().add(project.getUsers().iterator().next());

    user.setMetadata(defaultMetaData());

    userRepository.save(user);

    publishActivityEvents(user, project);

    return user;
  }

  private String checkUserName(String userName) {
    String regex = "^" + userName + "(_[0-9]+)?$";
    List<String> existingLogins = userRepository.findByLoginRegex(regex);

    if (existingLogins.isEmpty()) {
      return userName;
    }

    int maxPostfix = 0;
    for (String login : existingLogins) {
      if (login.equals(userName)) {
        continue;
      }
      String suffix = login.substring(userName.length() + 1);
      try {
        int num = Integer.parseInt(suffix);
        maxPostfix = Math.max(maxPostfix, num);
      } catch (NumberFormatException ignored) {
      }
    }
    return userName + "_" + (maxPostfix + 1);
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

  private void populateUserDetails(User user, Map<String, String> details) {
    String email = NORMALIZE_STRING.apply(details.get(UserAttribute.EMAIL.toString()));
    checkEmail(email);
    user.setEmail(email);
    String firstName = details.get(UserAttribute.FIRST_NAME.toString());
    String lastName = details.get(UserAttribute.LAST_NAME.toString());
    user.setFullName(String.join(" ", firstName, lastName));

    user.setRole(UserRole.USER);
  }

  private void populateUserDetailsIfSettingsArePresent(User user, Integration integration,
      Map<String, String> details) {

    String email = NORMALIZE_STRING.apply(
        details.get(SamlParameter.EMAIL_ATTRIBUTE.getParameter(integration).orElse(null)));
    checkEmail(email);
    user.setEmail(email);

    Optional<String> idpFullNameOptional =
        SamlParameter.FULL_NAME_ATTRIBUTE.getParameter(integration);

    if (idpFullNameOptional.isEmpty()) {
      String firstName = details.get(
          SamlParameter.FIRST_NAME_ATTRIBUTE.getParameter(integration).orElse(null));
      String lastName = details.get(
          SamlParameter.LAST_NAME_ATTRIBUTE.getParameter(integration).orElse(null));
      user.setFullName(String.join(" ", firstName, lastName));
    } else {
      String fullName = details.get(idpFullNameOptional.get());
      user.setFullName(fullName);
    }

    String roles = details.get(
        SamlParameter.ROLES_ATTRIBUTE.getParameter(integration).orElse(null));
    if (Objects.nonNull(roles) && roles.toLowerCase().contains("admin")) {
      user.setRole(UserRole.ADMINISTRATOR);
    } else {
      user.setRole(UserRole.USER);
    }
  }
}
