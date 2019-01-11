package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.converter.LdapConverter;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationStrategy;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractLdapResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class GetLdapStrategy implements GetAuthIntegrationStrategy {

	private final IntegrationRepository integrationRepository;

	@Autowired
	public GetLdapStrategy(IntegrationRepository integrationRepository) {
		this.integrationRepository = integrationRepository;
	}

	@Override
	public AbstractLdapResource getIntegration() {

		//or else empty integration with default 'enabled = false' flag
		return LdapConverter.TO_RESOURCE.apply(integrationRepository.findLdap().orElseGet(LdapConfig::new));
	}
}
