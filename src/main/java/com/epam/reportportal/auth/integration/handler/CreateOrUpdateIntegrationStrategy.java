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

package com.epam.reportportal.auth.integration.handler;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.builder.AuthIntegrationBuilder;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.enums.IntegrationGroupEnum;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public abstract class CreateOrUpdateIntegrationStrategy {

	private final IntegrationTypeRepository integrationTypeRepository;

	protected final IntegrationRepository integrationRepository;

	protected AuthIntegrationType type;

	private AuthIntegrationBuilder builder;

	private BiFunction<UpdateAuthRQ, Integration, Integration> fromResourceMapper;

	private Function<Integration, ? extends AbstractAuthResource> toResourceMapper;

	public CreateOrUpdateIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository) {
		this.integrationTypeRepository = integrationTypeRepository;
		this.integrationRepository = integrationRepository;
	}

	public AbstractAuthResource createOrUpdate(UpdateAuthRQ request, String username) {
		IntegrationType integrationType = initialize();
		Integration integration = find(request, integrationType).map(it -> update(request, it)).orElseGet(create(request, username));
		preProcess(integration);
		AbstractAuthResource resource = save(integration);
		postProcess(integration);
		destroy();
		return resource;
	}

	protected abstract void preProcess(Integration integration);

	protected abstract Optional<Integration> find(UpdateAuthRQ request, IntegrationType type);

	protected abstract void postProcess(Integration integration);

	protected abstract void beforeUpdate(UpdateAuthRQ request, Integration integration);

	protected abstract void beforeCreate(UpdateAuthRQ request);

	private Integration update(UpdateAuthRQ request, Integration integration) {
		validateIntegrationGroup(integration.getType().getIntegrationGroup());
		beforeUpdate(request, integration);
		return fromResourceMapper.apply(request, integration);
	}

	private Supplier<Integration> create(UpdateAuthRQ request, String username) {
		Preconditions.checkNotNull(builder);
		beforeCreate(request);
		return () -> builder.addCreator(username).addUpdateRq(request).build();
	}

	private AbstractAuthResource save(Integration integration) {
		return toResourceMapper.apply(integrationRepository.save(integration));
	}

	private IntegrationType initialize() {
		IntegrationType integrationType = integrationTypeRepository.findByName(type.getName())
				.orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, type.getName()));
		builder = type.getBuilder().addIntegrationType(integrationType);
		fromResourceMapper = type.getFromResourceMapper();
		toResourceMapper = type.getToResourceMapper();
		return integrationType;
	}

	private void destroy() {
		builder = null;
		fromResourceMapper = null;
		toResourceMapper = null;
	}

	private static void validateIntegrationGroup(IntegrationGroupEnum group) {
		BusinessRule.expect(group, equalTo(IntegrationGroupEnum.AUTH))
				.verify(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, "Wrong integration group");
	}
}
