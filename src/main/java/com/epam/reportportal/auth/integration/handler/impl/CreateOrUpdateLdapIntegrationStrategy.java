/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateOrUpdateIntegrationStrategy;
import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Component
public class CreateOrUpdateLdapIntegrationStrategy extends CreateOrUpdateIntegrationStrategy {

	private final BasicTextEncryptor encryptor;

	@Autowired
	public CreateOrUpdateLdapIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository, BasicTextEncryptor encryptor) {
		super(integrationTypeRepository, integrationRepository);
		this.encryptor = encryptor;
		this.type = AuthIntegrationType.LDAP;
	}

	@Override
	protected void preProcess(Integration integration) {

	}

	@Override
	protected Optional<Integration> find(UpdateAuthRQ request, IntegrationType type) {
		return integrationRepository.findExclusiveAuth(this.type.getName());
	}

	@Override
	protected void postProcess(Integration integration) {

	}

	@Override
	protected void beforeUpdate(UpdateAuthRQ request, Integration integration) {
		ParameterUtils.validateLdapRequest(request);
		LdapParameter.MANAGER_PASSWORD.getParameter(request)
				.ifPresent(fromRq -> request.getIntegrationParams()
						.put(LdapParameter.MANAGER_PASSWORD.getParameterName(), encryptor.encrypt(fromRq)));

	}

	@Override
	protected void beforeCreate(UpdateAuthRQ request) {
		ParameterUtils.validateLdapRequest(request);
		LdapParameter.MANAGER_PASSWORD.getParameter(request)
				.ifPresent(it -> request.getIntegrationParams()
						.put(LdapParameter.MANAGER_PASSWORD.getParameterName(), encryptor.encrypt(it)));
	}
}
