package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.event.SamlProvidersReloadEvent;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateOrUpdateIntegrationStrategy;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.*;
import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Component
public class CreateOrUpdateSamlIntegrationStrategy extends CreateOrUpdateIntegrationStrategy {

	private final SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning;

	private final ApplicationEventPublisher eventPublisher;

	@Autowired
	public CreateOrUpdateSamlIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository, SamlProviderProvisioning<ServiceProviderService> serviceProviderProvisioning,
			ApplicationEventPublisher eventPublisher) {
		super(integrationTypeRepository, integrationRepository);
		this.serviceProviderProvisioning = serviceProviderProvisioning;
		this.eventPublisher = eventPublisher;
		this.type = AuthIntegrationType.SAML;
	}

	@Override
	protected void preProcess(Integration integration) {
		populateProviderDetails(integration);
	}

	@Override
	protected Optional<Integration> find(UpdateAuthRQ request, IntegrationType type) {
		return integrationRepository.findByNameAndTypeId(IDP_NAME.getRequiredParameter(request), type.getId());
	}

	@Override
	protected void postProcess(Integration integration) {
		eventPublisher.publishEvent(new SamlProvidersReloadEvent(integrationRepository.findAllGlobalByType(integration.getType())));
	}

	@Override
	protected void beforeUpdate(UpdateAuthRQ request, Integration integration) {
		beforeCreate(request);
	}

	@Override
	protected void beforeCreate(UpdateAuthRQ request) {
		ParameterUtils.validateSamlRequest(request);
		if (FULL_NAME_ATTRIBUTE.getParameter(request).isEmpty() && (LAST_NAME_ATTRIBUTE.getParameter(request).isEmpty()
				&& FIRST_NAME_ATTRIBUTE.getParameter(request).isEmpty())) {
			throw new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
					"Fields Full name or combination of Last name and First name are empty"
			);
		}
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
