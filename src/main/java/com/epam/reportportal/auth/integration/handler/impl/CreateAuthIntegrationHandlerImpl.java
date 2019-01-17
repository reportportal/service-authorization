package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters;
import com.epam.reportportal.auth.integration.converter.OAuthRestrictionConverter;
import com.epam.reportportal.auth.integration.builder.ActiveDirectoryBuilder;
import com.epam.reportportal.auth.integration.builder.LdapBuilder;
import com.epam.reportportal.auth.integration.converter.ActiveDirectoryConverter;
import com.epam.reportportal.auth.integration.converter.LdapConverter;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.oauth.OAuthProviderFactory;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.enums.IntegrationAuthFlowEnum;
import com.epam.ta.reportportal.entity.enums.IntegrationGroupEnum;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateActiveDirectoryRQ;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class CreateAuthIntegrationHandlerImpl implements CreateAuthIntegrationHandler {

	private final IntegrationRepository integrationRepository;

	private final IntegrationTypeRepository integrationTypeRepository;

	private final MutableClientRegistrationRepository clientRegistrationRepository;

	@Autowired
	public CreateAuthIntegrationHandlerImpl(IntegrationRepository integrationRepository,
			IntegrationTypeRepository integrationTypeRepository, MutableClientRegistrationRepository clientRegistrationRepository) {
		this.integrationRepository = integrationRepository;
		this.integrationTypeRepository = integrationTypeRepository;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public LdapResource updateLdapSettings(UpdateLdapRQ updateLdapRQ) {
		LdapConfig ldapConfig = integrationRepository.findLdap().map(lc -> {
			BusinessRule.expect(lc.getType().getIntegrationGroup(), equalTo(IntegrationGroupEnum.AUTH))
					.verify(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, "Wrong integration group");
			return new LdapBuilder(lc).addUpdateRq(updateLdapRQ).build();
		}).orElseGet(() -> {
			LdapConfig config = new LdapBuilder().addUpdateRq(updateLdapRQ).build();
			updateWithAuthIntegrationParameters(config);
			return config;
		});

		return LdapConverter.TO_RESOURCE.apply(integrationRepository.save(ldapConfig));
	}

	@Override
	public ActiveDirectoryResource updateActiveDirectorySettings(UpdateActiveDirectoryRQ updateActiveDirectoryRQ) {

		ActiveDirectoryConfig activeDirectoryConfig = integrationRepository.findActiveDirectory().map(ad -> {
			BusinessRule.expect(ad.getType().getIntegrationGroup(), equalTo(IntegrationGroupEnum.AUTH))
					.verify(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, "Wrong integration group");
			return new ActiveDirectoryBuilder(ad).addUpdateRq(updateActiveDirectoryRQ).build();
		}).orElseGet(() -> {
			ActiveDirectoryConfig config = new ActiveDirectoryBuilder().addUpdateRq(updateActiveDirectoryRQ).build();
			updateWithAuthIntegrationParameters(config);
			return config;
		});

		return ActiveDirectoryConverter.TO_RESOURCE.apply(integrationRepository.save(activeDirectoryConfig));

	}

	@Override
	public OAuthRegistrationResource updateOauthSettings(String oauthProviderId, OAuthRegistrationResource clientRegistrationResource) {

		OAuthRegistration updatedOauthRegistration = clientRegistrationRepository.findOAuthRegistrationById(oauthProviderId)
				.map(existingRegistration -> updateRegistration(existingRegistration, clientRegistrationResource))
				.orElseGet(() -> OAuthProviderFactory.fillOAuthRegistration(oauthProviderId,
						OAuthRegistrationConverters.FROM_RESOURCE.apply(clientRegistrationResource)
				));

		return OAuthRegistrationConverters.TO_RESOURCE.apply(clientRegistrationRepository.save(updatedOauthRegistration));
	}

	private void updateWithAuthIntegrationParameters(Integration integration) {

		IntegrationType integrationType = integrationTypeRepository.findAllByIntegrationGroup(IntegrationGroupEnum.AUTH)
				.stream()
				.filter(it -> IntegrationAuthFlowEnum.LDAP.equals(it.getAuthFlow()))
				.findAny()
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, IntegrationAuthFlowEnum.LDAP.name()));

		integration.setType(integrationType);
	}

	private OAuthRegistration updateRegistration(OAuthRegistration existingRegistration,
			OAuthRegistrationResource clientRegistrationResource) {
		existingRegistration.setClientId(clientRegistrationResource.getClientId());
		existingRegistration.setClientSecret(clientRegistrationResource.getClientSecret());
		existingRegistration.setRestrictions(OAuthRestrictionConverter.FROM_RESOURCE.apply(clientRegistrationResource)
				.stream()
				.peek(restriction -> restriction.setRegistration(existingRegistration))
				.collect(Collectors.toSet()));
		return existingRegistration;
	}
}
