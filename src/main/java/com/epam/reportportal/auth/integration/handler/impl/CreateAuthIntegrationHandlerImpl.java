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

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters;
import com.epam.reportportal.auth.integration.converter.OAuthRestrictionConverter;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.CreateOrUpdateIntegrationStrategy;
import com.epam.reportportal.auth.oauth.OAuthProviderFactory;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class CreateAuthIntegrationHandlerImpl implements CreateAuthIntegrationHandler {

	private final MutableClientRegistrationRepository clientRegistrationRepository;

	private Map<AuthIntegrationType, CreateOrUpdateIntegrationStrategy> strategyMap;

	public CreateAuthIntegrationHandlerImpl(MutableClientRegistrationRepository clientRegistrationRepository,
			@Qualifier("createOrUpdateIntegrationStrategyMapping")
					Map<AuthIntegrationType, CreateOrUpdateIntegrationStrategy> strategyMap) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.strategyMap = strategyMap;
	}

	@Override
	public AbstractAuthResource createOrUpdateAuthSettings(UpdateAuthRQ request, AuthIntegrationType type, ReportPortalUser user) {
		return strategyMap.get(type).createOrUpdate(request, type, user.getUsername());
	}

	@Override
	public OAuthRegistrationResource createOrUpdateOauthSettings(String oauthProviderId,
			OAuthRegistrationResource clientRegistrationResource) {

		OAuthRegistration updatedOauthRegistration = clientRegistrationRepository.findOAuthRegistrationById(oauthProviderId)
				.map(existingRegistration -> updateRegistration(existingRegistration, clientRegistrationResource))
				.orElseGet(() -> OAuthProviderFactory.fillOAuthRegistration(oauthProviderId,
						OAuthRegistrationConverters.FROM_RESOURCE.apply(clientRegistrationResource)
				));

		return OAuthRegistrationConverters.TO_RESOURCE.apply(clientRegistrationRepository.save(updatedOauthRegistration));
	}

	private OAuthRegistration updateRegistration(OAuthRegistration existingRegistration,
			OAuthRegistrationResource clientRegistrationResource) {
		existingRegistration.setClientId(clientRegistrationResource.getClientId());
		existingRegistration.setClientSecret(clientRegistrationResource.getClientSecret());
		existingRegistration.setRestrictions(OAuthRestrictionConverter.FROM_RESOURCE.apply(clientRegistrationResource)
				.stream()
				.peek(restriction -> restriction.setRegistration(existingRegistration))
				.collect(toSet()));
		ofNullable(clientRegistrationResource.getScopes()).ifPresent(scopes -> existingRegistration.setScopes(scopes.stream()
				.map(OAuthRegistrationConverters.SCOPE_FROM_RESOURCE)
				.peek(scope -> scope.setRegistration(existingRegistration))
				.collect(toSet())));
		return existingRegistration;
	}
}
