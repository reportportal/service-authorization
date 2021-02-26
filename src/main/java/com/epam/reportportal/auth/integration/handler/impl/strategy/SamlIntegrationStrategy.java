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

package com.epam.reportportal.auth.integration.handler.impl.strategy;

import com.epam.reportportal.auth.event.SamlProvidersReloadEvent;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProvider;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.*;
import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Component
public class SamlIntegrationStrategy extends AuthIntegrationStrategy {

	private static final Predicate<UpdateAuthRQ> FULL_NAME_IS_EMPTY = request -> FULL_NAME_ATTRIBUTE.getParameter(request).isEmpty();
	private static final Predicate<UpdateAuthRQ> FIRST_AND_LAST_NAME_IS_EMPTY = request ->
			LAST_NAME_ATTRIBUTE.getParameter(request).isEmpty() && FIRST_NAME_ATTRIBUTE.getParameter(request).isEmpty();

	private final SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning;

	private final ApplicationEventPublisher eventPublisher;

	@Autowired
	public SamlIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository, IntegrationRepository integrationRepository,
			SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning, ApplicationEventPublisher eventPublisher) {
		super(integrationTypeRepository, integrationRepository, AuthIntegrationType.SAML);
		this.serviceProviderProvisioning = serviceProviderProvisioning;
		this.eventPublisher = eventPublisher;
	}

	@Override
	protected void validateRequest(UpdateAuthRQ request) {
		ParameterUtils.validateSamlRequest(request);
		BusinessRule.expect(FULL_NAME_IS_EMPTY.test(request) && FIRST_AND_LAST_NAME_IS_EMPTY.test(request), equalTo(Boolean.FALSE))
				.verify(ErrorType.BAD_REQUEST_ERROR, "Fields Full name or combination of Last name and First name are empty");
	}

	@Override
	protected void validateDuplicate(Integration integration, UpdateAuthRQ request) {
		getIntegrationRepository().findByNameAndTypeIdAndProjectIdIsNull(IDP_NAME.getRequiredParameter(request),
				integration.getType().getId()
		)
				.ifPresent(it -> BusinessRule.expect(it.getId(), id -> id.equals(integration.getId()))
						.verify(ErrorType.INTEGRATION_ALREADY_EXISTS, integration.getName()));
	}

	@Override
	protected AbstractAuthResource saveIntegration(Integration integration) {
		populateProviderDetails(integration);
		final AbstractAuthResource resource = super.saveIntegration(integration);
		final List<Integration> samlIntegrations = getIntegrationRepository().findAllGlobalByType(integration.getType());
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(samlIntegrations));
		return resource;
	}

	private void populateProviderDetails(Integration samlIntegration) {
		Map<String, Object> params = samlIntegration.getParams().getParams();
		ExternalIdentityProviderConfiguration externalConfiguration = new ExternalIdentityProviderConfiguration().setMetadata(SamlParameter.IDP_METADATA_URL
				.getRequiredParameter(samlIntegration));
		IdentityProviderMetadata remoteProvider = serviceProviderProvisioning.getHostedProvider().getRemoteProvider(externalConfiguration);
		params.put(IDP_URL.getParameterName(), remoteProvider.getEntityId());
		params.put(IDP_ALIAS.getParameterName(), remoteProvider.getEntityAlias());

		NameId nameId = ofNullable(remoteProvider.getDefaultNameId()).orElseGet(() -> {
			Optional<NameId> first = remoteProvider.getProviders()
					.stream()
					.filter(IdentityProvider.class::isInstance)
					.map(IdentityProvider.class::cast)
					.flatMap(v -> v.getNameIds().stream())
					.filter(Objects::nonNull)
					.findFirst();
			return first.orElseThrow(() -> new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
					"Provider does not contain information about identification mapping"
			));
		});
		params.put(IDP_NAME_ID.getParameterName(), nameId.toString());
	}
}
