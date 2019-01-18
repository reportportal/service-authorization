package com.epam.reportportal.auth.integration.handler;

import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateActiveDirectoryRQ;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public interface CreateAuthIntegrationHandler {

	LdapResource updateLdapSettings(UpdateLdapRQ updateLdapRQ);

	ActiveDirectoryResource updateActiveDirectorySettings(UpdateActiveDirectoryRQ updateActiveDirectoryRQ);

	OAuthRegistrationResource updateOauthSettings(String oauthProviderId, OAuthRegistrationResource clientRegistrationResource);
}
