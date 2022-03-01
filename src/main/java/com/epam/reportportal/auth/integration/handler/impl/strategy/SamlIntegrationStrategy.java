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
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.validator.duplicate.IntegrationDuplicateValidator;
import com.epam.reportportal.auth.integration.validator.request.AuthRequestValidator;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.integration.IntegrationTypeDetails;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProvider;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.epam.reportportal.auth.integration.converter.SamlConverter.UPDATE_FROM_REQUEST;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.*;
import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
public class SamlIntegrationStrategy extends AuthIntegrationStrategy {

	private final SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning;
	private final ApplicationEventPublisher eventPublisher;

	@Autowired
	public SamlIntegrationStrategy(IntegrationRepository integrationRepository,
			@Qualifier("samlUpdateAuthRequestValidator") AuthRequestValidator<UpdateAuthRQ> updateAuthRequestValidator,
			IntegrationDuplicateValidator integrationDuplicateValidator,
			SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning, ApplicationEventPublisher eventPublisher) {
		super(integrationRepository, updateAuthRequestValidator, integrationDuplicateValidator);
		this.serviceProviderProvisioning = serviceProviderProvisioning;
		this.eventPublisher = eventPublisher;
	}

	@Override
	protected void fill(Integration integration, UpdateAuthRQ updateRequest) {
		UPDATE_FROM_REQUEST.accept(updateRequest, integration);
		BASE_PATH.getParameter(updateRequest).ifPresent(basePath -> {
			validateBasePath(basePath);
			updateBasePath(integration, basePath);
		});
	}

	private void validateBasePath(String basePath) {
		if (!UrlValidator.getInstance().isValid(basePath)) {
			throw new ReportPortalException(ErrorType.BAD_REQUEST_ERROR, "callbackUrl is invalid");
		}
	}

	private void updateBasePath(Integration integration, String basePath) {
		final IntegrationType integrationType = integration.getType();
		final IntegrationTypeDetails typeDetails = ofNullable(integrationType.getDetails()).orElseGet(() -> {
			final IntegrationTypeDetails details = new IntegrationTypeDetails();
			integrationType.setDetails(details);
			return details;
		});
		final Map<String, Object> detailsMapping = ofNullable(typeDetails.getDetails()).orElseGet(() -> {
			final Map<String, Object> details = new HashMap<>();
			typeDetails.setDetails(details);
			return details;
		});
		detailsMapping.put(BASE_PATH.getParameterName(), basePath);
	}

	@Override
	protected Integration save(Integration integration) {
		populateProviderDetails(integration);
		final Integration result = super.save(integration);
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(result.getType()));
		return result;
	}

	private void populateProviderDetails(Integration samlIntegration) {
		Map<String, Object> params = samlIntegration.getParams().getParams();
		ExternalIdentityProviderConfiguration externalConfiguration = new ExternalIdentityProviderConfiguration().setMetadata(SamlParameter.IDP_METADATA_URL.getRequiredParameter(
				samlIntegration));
		IdentityProviderMetadata remoteProvider = serviceProviderProvisioning.getHostedProvider().getRemoteProvider(externalConfiguration);
		params.put(IDP_URL.getParameterName(), remoteProvider.getEntityId());
		params.put(IDP_ALIAS.getParameterName(), remoteProvider.getEntityAlias());

		NameId nameId = ofNullable(remoteProvider.getDefaultNameId()).orElseGet(() -> remoteProvider.getProviders()
				.stream()
				.filter(IdentityProvider.class::isInstance)
				.map(IdentityProvider.class::cast)
				.flatMap(v -> v.getNameIds().stream())
				.filter(Objects::nonNull)
				.findFirst().orElse(NameId.UNSPECIFIED));

		params.put(IDP_NAME_ID.getParameterName(), nameId.toString());
	}
}
