package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.converter.ActiveDirectoryConverter;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationStrategy;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class GetActiveDirectoryStrategy implements GetAuthIntegrationStrategy {

	private final IntegrationRepository integrationRepository;

	@Autowired
	public GetActiveDirectoryStrategy(IntegrationRepository integrationRepository) {
		this.integrationRepository = integrationRepository;
	}

	@Override
	public ActiveDirectoryResource getIntegration() {

		//or else empty integration with default 'enabled = false' flag
		return ActiveDirectoryConverter.TO_RESOURCE.apply(integrationRepository.findActiveDirectory()
				.orElseGet(ActiveDirectoryConfig::new));
	}
}
