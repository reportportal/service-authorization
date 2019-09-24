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

package com.epam.reportportal.auth.integration.core;

import com.epam.reportportal.extension.event.IntegrationEvent;
import com.epam.reportportal.extension.event.IntegrationEventType;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationParams;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.integration.IntegrationRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class CreateIntegrationHandlerImpl implements CreateIntegrationHandler {

	private final IntegrationRepository integrationRepository;

	private final IntegrationTypeRepository integrationTypeRepository;

	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public CreateIntegrationHandlerImpl(IntegrationRepository integrationRepository, IntegrationTypeRepository integrationTypeRepository,
			ApplicationEventPublisher applicationEventPublisher) {
		this.integrationRepository = integrationRepository;
		this.integrationTypeRepository = integrationTypeRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public EntryCreatedRS createGlobalIntegration(IntegrationRQ createRequest, String pluginName, ReportPortalUser user) {
		IntegrationType integrationType = integrationTypeRepository.findByName(pluginName)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, pluginName));
		Integration integration = createIntegration(createRequest, integrationType);
		integration.setCreator(user.getUsername());
		integrationRepository.save(integration);
		publishEvent(integration, IntegrationEventType.CREATED);
		return new EntryCreatedRS(integration.getId());

	}

	@Override
	public OperationCompletionRS updateGlobalIntegration(Long id, IntegrationRQ updateRequest) {
		Integration integration = integrationRepository.findGlobalById(id)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, id));
		updateIntegration(integration, updateRequest);
		integrationRepository.save(integration);
		publishEvent(integration, IntegrationEventType.UPDATED);

		return new OperationCompletionRS("Integration with id = " + integration.getId() + " has been successfully updated.");
	}

	private IntegrationParams getIntegrationParams(Integration integration, Map<String, Object> retrievedParams) {
		if (integration.getParams() != null && integration.getParams().getParams() != null) {
			integration.getParams().getParams().putAll(retrievedParams);
			return integration.getParams();
		}
		return new IntegrationParams(retrievedParams);
	}

	private Integration createIntegration(IntegrationRQ integrationRq, IntegrationType integrationType) {
		Integration integration = new Integration();
		integration.setCreationDate(LocalDateTime.now());
		Map<String, Object> integrationParams = integrationRq.getIntegrationParams();
		integration.setParams(new IntegrationParams(integrationParams));
		integration.setType(integrationType);
		integration.setEnabled(integrationRq.getEnabled());
		integration.setName(integrationRq.getName());
		return integration;
	}

	private Integration updateIntegration(Integration integration, IntegrationRQ integrationRQ) {
		Map<String, Object> integrationParams = integrationRQ.getIntegrationParams();
		IntegrationParams params = getIntegrationParams(integration, integrationParams);
		integration.setParams(params);
		Optional.ofNullable(integrationRQ.getEnabled()).ifPresent(integration::setEnabled);
		Optional.ofNullable(integrationRQ.getName()).ifPresent(integration::setName);
		return integration;
	}

	private void publishEvent(Integration integration, IntegrationEventType eventType) {
		applicationEventPublisher.publishEvent(new IntegrationEvent(Collections.singletonList(integration.getId()),
				integration.getType().getName(),
				eventType
		));
	}

}
