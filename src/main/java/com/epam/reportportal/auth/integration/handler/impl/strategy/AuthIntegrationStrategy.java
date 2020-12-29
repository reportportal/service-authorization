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

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;

import java.util.Optional;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public abstract class AuthIntegrationStrategy {

	private final IntegrationTypeRepository integrationTypeRepository;
	private final IntegrationRepository integrationRepository;
	private final AuthIntegrationType type;

	public AuthIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository, AuthIntegrationType type) {
		this.integrationTypeRepository = integrationTypeRepository;
		this.integrationRepository = integrationRepository;
		this.type = type;
	}

	protected abstract void validateRequest(UpdateAuthRQ request);

	protected abstract void validateDuplicate(Integration integration, UpdateAuthRQ request);

	public AbstractAuthResource createIntegration(UpdateAuthRQ request, String username) {
		validateRequest(request);

		IntegrationType integrationType = integrationTypeRepository.findByName(type.getName())
				.orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));
		final Integration integration = type.getBuilder()
				.addIntegrationType(integrationType)
				.addCreator(username)
				.addUpdateRq(request)
				.build();

		BusinessRule.expect(integrationRepository.findByNameAndTypeIdAndProjectIdIsNull(integration.getName(), integrationType.getId()),
				Optional::isEmpty
		).verify(ErrorType.INTEGRATION_ALREADY_EXISTS, integration.getName());

		return saveIntegration(integration);
	}

	public AbstractAuthResource updateIntegration(Long integrationId, UpdateAuthRQ request) {
		validateRequest(request);

		IntegrationType integrationType = integrationTypeRepository.findByName(type.getName())
				.orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));
		final Integration integration = integrationRepository.findByIdAndTypeIdAndProjectIdIsNull(integrationId, integrationType.getId())
				.orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));

		validateDuplicate(integration, request);

		return saveIntegration(type.getFromResourceMapper().apply(request, integration));
	}

	protected AbstractAuthResource saveIntegration(Integration integration) {
		return type.getToResourceMapper().apply(integrationRepository.save(integration));
	}

	protected IntegrationTypeRepository getIntegrationTypeRepository() {
		return integrationTypeRepository;
	}

	protected IntegrationRepository getIntegrationRepository() {
		return integrationRepository;
	}

	protected AuthIntegrationType getType() {
		return type;
	}
}
