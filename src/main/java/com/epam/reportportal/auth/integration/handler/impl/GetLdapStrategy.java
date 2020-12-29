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
import com.epam.reportportal.auth.integration.converter.LdapConverter;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationStrategy;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractLdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class GetLdapStrategy implements GetAuthIntegrationStrategy {

	private final IntegrationTypeRepository integrationTypeRepository;

	private final IntegrationRepository integrationRepository;

	@Autowired
	public GetLdapStrategy(IntegrationTypeRepository integrationTypeRepository, IntegrationRepository integrationRepository) {
		this.integrationTypeRepository = integrationTypeRepository;
		this.integrationRepository = integrationRepository;
	}

	@Override
	public AbstractLdapResource getIntegration() {
		IntegrationType ldapIntegrationType = integrationTypeRepository.findByName(AuthIntegrationType.LDAP.getName())
				.orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, AuthIntegrationType.LDAP.getName()));

		//or else empty integration with default 'enabled = false' flag
		LdapResource ldapResource = LdapConverter.TO_RESOURCE.apply(integrationRepository.findByNameAndTypeIdAndProjectIdIsNull(AuthIntegrationType.LDAP.getName(),
				ldapIntegrationType.getId()
		).orElseGet(Integration::new));
		ldapResource.setType(ldapIntegrationType.getName());
		return ldapResource;
	}
}
