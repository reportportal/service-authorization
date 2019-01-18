/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.DeleteAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationHandler;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Endpoint for oauth configs
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Controller
@RequestMapping("/settings/oauth")
@Api(description = "OAuth Configuration Endpoint")
public class OAuthConfigurationEndpoint {

	private final CreateAuthIntegrationHandler createAuthIntegrationHandler;

	private final DeleteAuthIntegrationHandler deleteAuthIntegrationHandler;

	private final GetAuthIntegrationHandler getAuthIntegrationHandler;

	@Autowired
	public OAuthConfigurationEndpoint(CreateAuthIntegrationHandler createAuthIntegrationHandler,
			DeleteAuthIntegrationHandler deleteAuthIntegrationHandler, GetAuthIntegrationHandler getAuthIntegrationHandler) {
		this.createAuthIntegrationHandler = createAuthIntegrationHandler;
		this.deleteAuthIntegrationHandler = deleteAuthIntegrationHandler;
		this.getAuthIntegrationHandler = getAuthIntegrationHandler;
	}

	/**
	 * Updates oauth integration settings
	 *
	 * @param clientRegistrationResource OAuth configuration
	 * @return All defined OAuth integration settings
	 */
	@Transactional
	@RequestMapping(value = "/{authId}", method = { POST, PUT })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Creates/Updates OAuth Integration Settings")
	public OAuthRegistrationResource updateOAuthSettings(@PathVariable("authId") String oauthProviderId,
			@RequestBody @Validated OAuthRegistrationResource clientRegistrationResource) {
		return createAuthIntegrationHandler.updateOauthSettings(oauthProviderId, clientRegistrationResource);
	}

	/**
	 * Deletes oauth integration settings
	 *
	 * @param oauthProviderId Oauth settings Profile Id
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/{authId}", method = { DELETE })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Deletes OAuth Integration Settings")
	public OperationCompletionRS deleteOAuthSetting(@PathVariable("authId") String oauthProviderId) {
		return deleteAuthIntegrationHandler.deleteOauthSettingsById(oauthProviderId);
	}

	/**
	 * Returns oauth integration settings
	 *
	 * @return All defined OAuth integration settings
	 */
	@GetMapping
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Returns OAuth Server Settings")
	public Map<String, OAuthRegistrationResource> getOAuthSettings() {
		return getAuthIntegrationHandler.getAllOauthIntegrations();
	}

	/**
	 * Returns oauth integration settings
	 *
	 * @param oauthProviderId ID of third-party OAuth provider
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/{authId}", method = { GET })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Returns OAuth Server Settings")
	public OAuthRegistrationResource getOAuthSettings(@PathVariable("authId") String oauthProviderId) {
		return getAuthIntegrationHandler.getOauthIntegrationById(oauthProviderId);
	}
}