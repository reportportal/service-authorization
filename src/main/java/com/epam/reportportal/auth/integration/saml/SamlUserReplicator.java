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

import static com.epam.reportportal.auth.util.AuthUtils.NORMALIZE_STRING;

import com.epam.reportportal.auth.binary.UserBinaryDataService;
import com.epam.reportportal.auth.commons.ContentTypeResolver;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.dao.ProjectRepository;
import com.epam.reportportal.auth.dao.UserRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.entity.user.UserRole;
import com.epam.reportportal.auth.entity.user.UserType;
import com.epam.reportportal.auth.event.UserEventPublisher;
import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.reportportal.auth.util.PersonalProjectService;
import jakarta.persistence.NonUniqueResultException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replicates user from a validated SAML assertion into database if it is not exist.
 *
 * <p>This class must only be given attributes of an assertion that has already been validated by
 * Spring Security's SAML authentication provider (see {@link SamlResponseAuthenticationConverter}).
 * It never parses SAML XML itself.
 *
 * @author <a href="mailto:Reingold_Shekhtel@epam.com">Reingold Shekhtel</a>
 */
@Slf4j
@Component
public class SamlUserReplicator extends AbstractUserReplicator {

  private static final String USER_ALREADY_EXISTS_MSG = "User with login '%s' already exists";
  private final IntegrationTypeRepository integrationTypeRepository;
  private final IntegrationRepository integrationRepository;
  private final UserEventPublisher userEventPublisher;

  /**
   * SAML user replicator constructor.
   *
   * @param userRepository            User repository
   * @param projectRepository         Project repository
   * @param personalProjectService    Personal project service
   * @param userBinaryDataService     User binary data service
   * @param integrationTypeRepository Integration type repository
   * @param integrationRepository     Integration repository
   * @param contentTypeResolver       Content type resolver
   */
  @Autowired
  public SamlUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
      PersonalProjectService personalProjectService, UserBinaryDataService userBinaryDataService,
      IntegrationTypeRepository integrationTypeRepository,
      IntegrationRepository integrationRepository, ContentTypeResolver contentTypeResolver,
      UserEventPublisher userEventPublisher) {
    super(userRepository, projectRepository, personalProjectService, userBinaryDataService,
        contentTypeResolver
    );
    this.integrationTypeRepository = integrationTypeRepository;
    this.integrationRepository = integrationRepository;
    this.userEventPublisher = userEventPublisher;
  }

  /**
   * Finds the user asserted by the identity provider or creates it if it does not exist.
   *
   * <p>The user is identified by the value of the integration's email attribute, for both lookup
   * and creation. An existing account is accepted only if it is an active account of type
   * {@link UserType#SAML}.
   *
   * @param registrationId Relying party registration id (identity provider name) the response was
   *                       validated against
   * @param attributes     Attributes of the validated assertion
   * @return {@link User}
   */
  @Transactional
  public User replicateUser(String registrationId, Map<String, List<Object>> attributes) {
    var integration = findProvider(registrationId);
    var userEmail = resolveEmail(attributes, integration);

    Optional<User> userOptional;

    try {
      userOptional = userRepository.findByEmailIgnoreCase(userEmail);
    } catch (NonUniqueResultException e) {
      log.error("Data integrity violation: Multiple users found with email: {}", userEmail);
      throw new UserSynchronizationException("User with email '"
          + userEmail
          + "' already exists, but multiple records found. Please contact administrator to resolve this issue.");
    }

    if (userOptional.isPresent()) {
      var user = userOptional.get();
      checkUserType(user);
      checkUserActive(user);
      return user;
    }

    return createUser(userEmail, attributes, integration);
  }

  private void checkUserType(User user) {
    if (!UserType.SAML.equals(user.getUserType())) {
      throw new UserSynchronizationException(
          String.format(USER_ALREADY_EXISTS_MSG, user.getLogin()));
    }
  }

  private void checkUserActive(User user) {
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new LockedException("User account is locked");
    }
  }

  private Integration findProvider(String registrationId) {
    var integrationType = integrationTypeRepository.findByName(AuthIntegrationType.SAML.getName())
        .orElseThrow(() -> new UserSynchronizationException("SAML integration type not found"));

    // Disabled integrations are refused here as well, so that a stale relying party registration
    // (e.g. on another instance that has not reloaded yet) cannot be used to log in.
    return integrationRepository.findAllGlobalByType(integrationType)
        .stream()
        .filter(Integration::isEnabled)
        .filter(integration -> SamlParameter.IDP_NAME.getParameter(integration)
            .filter(name -> name.equals(registrationId))
            .isPresent())
        .findFirst()
        .orElseThrow(() -> new UserSynchronizationException(
            "SAML provider not found or disabled. Check SAML integration."));
  }

  private User createUser(String email, Map<String, List<Object>> details,
      Integration integration) {
    User user = new User();
    user.setLogin(email);
    user.setEmail(email);
    user.setUuid(UUID.randomUUID());
    user.setFullName(resolveName(details, integration));
    user.setActive(Boolean.TRUE);
    user.setUserType(UserType.SAML);
    user.setRole(resolveRole(details, integration));
    user.setExpired(false);

    user.setMetadata(defaultMetaData());

    var saved = userRepository.save(user);
    userEventPublisher.publishOnUserCreated(saved);

    return saved;
  }

  private String resolveEmail(Map<String, List<Object>> details, Integration integration) {
    var attr = SamlParameter.EMAIL_ATTRIBUTE
        .getParameter(integration)
        .orElse(UserAttribute.EMAIL.toString());

    return Optional.ofNullable(firstValue(details, attr))
        .filter(StringUtils::isNotBlank)
        .map(NORMALIZE_STRING)
        .orElseThrow(() -> new UserSynchronizationException(
            "SAML response does not contain email attribute '" + attr + "'"));
  }

  private String resolveName(Map<String, List<Object>> details, Integration integration) {
    Optional<String> fullNameAttr = SamlParameter.FULL_NAME_ATTRIBUTE.getParameter(integration);

    if (fullNameAttr.isPresent()) {
      return firstValue(details, fullNameAttr.get());
    }

    var firstNameAttr = SamlParameter.FIRST_NAME_ATTRIBUTE.getParameter(integration)
        .orElse(UserAttribute.FIRST_NAME.toString());
    var lastNameAttr = SamlParameter.LAST_NAME_ATTRIBUTE.getParameter(integration)
        .orElse(UserAttribute.LAST_NAME.toString());

    return String.join(" ", firstValue(details, firstNameAttr), firstValue(details, lastNameAttr));
  }

  /**
   * Maps the roles attribute to a ReportPortal role. The user becomes an administrator only if one
   * of the attribute values is exactly one of the integration's configured administrator role
   * values ({@code ADMINISTRATOR} by default), compared case-insensitively after trimming. Any
   * other value results in a regular user.
   */
  private UserRole resolveRole(Map<String, List<Object>> details, Integration integration) {
    var attr = SamlParameter.ROLES_ATTRIBUTE
        .getParameter(integration)
        .orElse(UserAttribute.ROLES.toString());
    var adminValues = SamlParameter.getAdminRoleValues(integration);

    return values(details, attr).stream()
        .map(String::trim)
        .anyMatch(value -> adminValues.stream().anyMatch(value::equalsIgnoreCase))
        ? UserRole.ADMINISTRATOR : UserRole.USER;
  }

  private static List<String> values(Map<String, List<Object>> details, String attribute) {
    return Optional.ofNullable(details.get(attribute)).orElseGet(List::of)
        .stream()
        .filter(Objects::nonNull)
        .map(Object::toString)
        .toList();
  }

  private static String firstValue(Map<String, List<Object>> details, String attribute) {
    return values(details, attribute).stream().findFirst().orElse(null);
  }
}
