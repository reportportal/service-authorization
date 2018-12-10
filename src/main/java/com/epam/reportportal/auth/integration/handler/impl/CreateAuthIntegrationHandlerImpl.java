package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.builder.ActiveDirectoryBuilder;
import com.epam.reportportal.auth.integration.builder.LdapBuilder;
import com.epam.reportportal.auth.integration.converter.ActiveDirectoryConverter;
import com.epam.reportportal.auth.integration.converter.LdapConverter;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.enums.IntegrationGroupEnum;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateActiveDirectoryRQ;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class CreateAuthIntegrationHandlerImpl implements CreateAuthIntegrationHandler {

	private final IntegrationRepository integrationRepository;

	private final IntegrationTypeRepository integrationTypeRepository;

	@Autowired
	public CreateAuthIntegrationHandlerImpl(IntegrationRepository integrationRepository, IntegrationTypeRepository integrationTypeRepository) {
		this.integrationRepository = integrationRepository;
		this.integrationTypeRepository = integrationTypeRepository;
	}

	@Override
	public LdapResource updateLdapSettings(UpdateLdapRQ updateLdapRQ) {
		LdapConfig ldapConfig = integrationRepository.findLdap().map(lc -> {
			BusinessRule.expect(lc.getType().getIntegrationGroup(), equalTo(IntegrationGroupEnum.AUTH))
					.verify(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, "Wrong integration group");
			return new LdapBuilder(lc).addUpdateRq(updateLdapRQ).build();
		}).orElseGet(() -> {
			LdapConfig lc = new LdapBuilder().addUpdateRq(updateLdapRQ).build();
			updateWithAuthIntegrationParameters(lc);
			return lc;
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

	private void updateWithAuthIntegrationParameters(Integration integration) {

		IntegrationType integrationType = integrationTypeRepository.findByIntegrationGroup(IntegrationGroupEnum.AUTH)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND,
						"Authentication integrations haven't been found"
				));

		integration.setType(integrationType);
	}
}
