package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateOrUpdateIntegrationStrategy;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Component
public class CreateOrUpdateActiveDirectoryIntegrationStrategy extends CreateOrUpdateIntegrationStrategy {

	@Autowired
	public CreateOrUpdateActiveDirectoryIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository) {
		super(integrationTypeRepository, integrationRepository);
		this.type = AuthIntegrationType.ACTIVE_DIRECTORY;
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
	}

	@Override
	protected void beforeCreate(UpdateAuthRQ request) {
		ParameterUtils.validateLdapRequest(request);
	}
}
