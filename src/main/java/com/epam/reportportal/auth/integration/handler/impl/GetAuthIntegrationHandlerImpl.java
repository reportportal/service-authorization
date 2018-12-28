package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationStrategy;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractLdapResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class GetAuthIntegrationHandlerImpl implements GetAuthIntegrationHandler {

	private final Map<AuthIntegrationType, GetAuthIntegrationStrategy> authIntegrationStrategyMapping;

	@Autowired
	public GetAuthIntegrationHandlerImpl(@Qualifier(value = "authIntegrationStrategyMapping")
			Map<AuthIntegrationType, GetAuthIntegrationStrategy> authIntegrationStrategyMapping) {
		this.authIntegrationStrategyMapping = authIntegrationStrategyMapping;
	}

	@Override
	public AbstractLdapResource getIntegrationByType(AuthIntegrationType integrationType) {
		return ofNullable(authIntegrationStrategyMapping.get(integrationType)).orElseThrow(() -> new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
				"Unable to find suitable auth integration strategy for type= " + integrationType
		))
				.getIntegration();
	}
}
