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
import com.epam.ta.reportportal.commons.validation.Suppliers;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Service
public class DeleteIntegrationHandlerImpl implements DeleteIntegrationHandler {

	private final IntegrationRepository integrationRepository;

	private final IntegrationTypeRepository integrationTypeRepository;

	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public DeleteIntegrationHandlerImpl(IntegrationRepository integrationRepository, IntegrationTypeRepository integrationTypeRepository,
			ApplicationEventPublisher applicationEventPublisher) {
		this.integrationRepository = integrationRepository;
		this.integrationTypeRepository = integrationTypeRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public OperationCompletionRS deleteGlobalIntegration(Long integrationId) {
		Integration integration = integrationRepository.findGlobalById(integrationId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, integrationId));
		integrationRepository.deleteById(integration.getId());
		applicationEventPublisher.publishEvent(new IntegrationEvent(Collections.singletonList(integration.getId()),
				integration.getType().getName(),
				IntegrationEventType.DELETED
		));

		return new OperationCompletionRS(Suppliers.formattedSupplier("Global integration with id = {} has been successfully removed",
				integration.getId()
		).get());
	}

	@Override
	public OperationCompletionRS deleteGlobalIntegrationsByType(String type) {
		IntegrationType integrationType = integrationTypeRepository.findByName(type)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, type));
		List<Long> integrationIds = integrationRepository.findAllGlobalByType(integrationType)
				.stream()
				.map(Integration::getId)
				.collect(toList());
		integrationRepository.deleteAllGlobalByIntegrationTypeId(integrationType.getId());
		applicationEventPublisher.publishEvent(new IntegrationEvent(integrationIds,
				integrationType.getName(),
				IntegrationEventType.DELETED
		));

		return new OperationCompletionRS(
				"All global integrations with type ='" + integrationType.getName() + "' integrations have been successfully removed.");
	}
}
