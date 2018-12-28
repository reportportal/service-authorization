package com.epam.reportportal.auth.integration.handler;

import com.epam.ta.reportportal.ws.model.integration.auth.AbstractLdapResource;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public interface GetAuthIntegrationStrategy {

	AbstractLdapResource getIntegration();
}
