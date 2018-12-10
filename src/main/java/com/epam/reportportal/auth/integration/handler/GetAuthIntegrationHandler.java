package com.epam.reportportal.auth.integration.handler;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.ta.reportportal.entity.integration.Integration;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public interface GetAuthIntegrationHandler {

	Integration getIntegrationByType(AuthIntegrationType integrationType);
}
