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

package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.commons.ReportPortalUser;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.entity.oauth.OAuthRegistration;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.impl.strategy.AuthIntegrationStrategy;
import com.epam.reportportal.auth.integration.provider.AuthIntegrationStrategyProvider;
import com.epam.reportportal.auth.model.integration.auth.AbstractAuthResource;
import com.epam.reportportal.auth.model.integration.auth.UpdateAuthRQ;
import com.epam.reportportal.auth.model.settings.OAuthRegistrationResource;
import com.epam.reportportal.auth.oauth.OAuthProviderFactory;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class CreateAuthIntegrationHandlerImpl implements CreateAuthIntegrationHandler {

  private final MutableClientRegistrationRepository clientRegistrationRepository;
  private final AuthIntegrationStrategyProvider strategyProvider;
  private final IntegrationTypeRepository integrationTypeRepository;

  @Value("${server.servlet.context-path}")
  private String pathValue;

  @Autowired
  public CreateAuthIntegrationHandlerImpl(
      MutableClientRegistrationRepository clientRegistrationRepository,
      AuthIntegrationStrategyProvider strategyProvider,
      IntegrationTypeRepository integrationTypeRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.strategyProvider = strategyProvider;
    this.integrationTypeRepository = integrationTypeRepository;
  }

  @Override
  public AbstractAuthResource createAuthIntegration(AuthIntegrationType type, UpdateAuthRQ request,
      ReportPortalUser user) {
    final IntegrationType integrationType = getIntegrationType(type);
    final AuthIntegrationStrategy authIntegrationStrategy = getAuthStrategy(type);
    final Integration integration = authIntegrationStrategy.createIntegration(integrationType,
        request, user.getUsername());
    return type.getToResourceMapper().apply(integration);
  }

  @Override
  public AbstractAuthResource updateAuthIntegration(AuthIntegrationType type, Long integrationId,
      UpdateAuthRQ request,
      ReportPortalUser user) {
    final IntegrationType integrationType = getIntegrationType(type);
    final AuthIntegrationStrategy authIntegrationStrategy = getAuthStrategy(type);
    final Integration integration = authIntegrationStrategy.updateIntegration(integrationType,
        integrationId, request);
    return type.getToResourceMapper().apply(integration);
  }

  private IntegrationType getIntegrationType(AuthIntegrationType type) {
    return integrationTypeRepository.findByName(type.getName())
        .orElseThrow(
            () -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));
  }

  private AuthIntegrationStrategy getAuthStrategy(AuthIntegrationType type) {
    return strategyProvider.provide(type)
        .orElseThrow(
            () -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));
  }

  @Override
  public OAuthRegistrationResource createOrUpdateOauthSettings(String oauthProviderId,
      OAuthRegistrationResource clientRegistrationResource) {

    Optional<OAuthRegistration> existing =
        clientRegistrationRepository.findOAuthRegistrationById(oauthProviderId);
    if (StringUtils.isBlank(clientRegistrationResource.getClientSecret())) {
      // the secret is write-only: keep the stored one when the client does not send it
      String storedSecret = existing.map(OAuthRegistration::getClientSecret)
          .filter(StringUtils::isNotBlank)
          .orElseThrow(() -> new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
              "clientSecret must be provided"));
      clientRegistrationResource.setClientSecret(storedSecret);
    }

    OAuthRegistration oAuthRegistration = OAuthProviderFactory.fillOAuthRegistration(
        oauthProviderId, clientRegistrationResource, pathValue);

    OAuthRegistration updatedOauthRegistration =
        existing
            .map(existingRegistration -> {
              clientRegistrationRepository.deleteById(existingRegistration.getId());
              oAuthRegistration.setId(existingRegistration.getId());
              return oAuthRegistration;
            })
            .orElse(oAuthRegistration);

    return OAuthRegistrationConverters.TO_RESOURCE.apply(
        clientRegistrationRepository.save(updatedOauthRegistration));
  }

}
