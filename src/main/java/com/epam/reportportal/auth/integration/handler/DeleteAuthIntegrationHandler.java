package com.epam.reportportal.auth.integration.handler;

import com.epam.ta.reportportal.ws.model.OperationCompletionRS;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public interface DeleteAuthIntegrationHandler {

	OperationCompletionRS deleteAuthIntegrationById(Long integrationId);
}
