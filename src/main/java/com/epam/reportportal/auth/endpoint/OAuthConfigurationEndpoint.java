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

import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Endpoint for oauth configs
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Controller
@RequestMapping("/settings/{profileId}/oauth")
@Api(description = "OAuth Configuration Endpoint")
public class OAuthConfigurationEndpoint {

//	private final ClientRegistrationService clientRegistrationService;
//	private final Map<String, OAuthProvider> providers;

//	@Autowired
//	public OAuthConfigurationEndpoint(ClientRegistrationService clientRegistrationService
////			Map<String, OAuthProvider> providers
//	) {
//		this.clientRegistrationService = clientRegistrationService;
//
////		this.providers = providers;
//	}

//	/**
//	 * Updates oauth integration settings
//	 *
//	 * @param profileId         settings ProfileID
//	 * @param oauthDetails      OAuth details resource update
//	 * @param oauthProviderName ID of third-party OAuth provider
//	 * @return All defined OAuth integration settings
//	 */
//	@RequestMapping(value = "/{authId}", method = { POST, PUT })
//	@ResponseBody
//	@ResponseStatus(HttpStatus.OK)
//	@ApiOperation(value = "Updates OAuth Integration Settings", notes = "'default' profile is using till additional UI implementations")
//	public Map<String, ClientDetails> updateOAuthSettings(@PathVariable String profileId, @PathVariable("authId") String oauthProviderName,
//			@RequestBody @Validated BaseClientDetails oauthDetails) {
//		clientRegistrationService.updateClientDetails(oauthDetails);
//		return getOAuthSettings();
//	}
//
//	/**
//	 * Deletes oauth integration settings
//	 *
//	 * @param clientID          settings ProfileID
//	 * @param oauthProviderName ID of third-party OAuth provider
//	 * @return All defined OAuth integration settings
//	 */
//	@RequestMapping(value = "/{authId}", method = { DELETE })
//	@ResponseBody
//	@ResponseStatus(HttpStatus.OK)
//	@ApiOperation(value = "Deletes OAuth Integration Settings", notes = "'default' profile is using till additional UI implementations")
//	public OperationCompletionRS deleteOAuthSetting(@PathVariable("authId") String clientID) {
//		try {
//			clientRegistrationService.removeClientDetails(clientID);
//		} catch (NoSuchClientException e) {
//			throw new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND);
//		}
//
//		return new OperationCompletionRS("Auth settings '" + clientID + "' were successfully removed");
//	}
//
//	/**
//	 * Returns oauth integration settings
//	 *
//	 * @return All defined OAuth integration settings
//	 */
//	@RequestMapping(value = "/", method = { GET })
//	@ResponseBody
//	@ResponseStatus(HttpStatus.OK)
//	@ApiOperation(value = "Returns OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
//	public Map<String, ClientDetails> getOAuthSettings() {
//		return clientRegistrationService.listClientDetails().stream().collect(Collectors.toMap(ClientDetails::getClientId, d -> d));
//	}
//
//	//	/**
//	//	 * Returns oauth integration settings
//	//	 *
//	//	 * @param profileId         settings ProfileID
//	//	 * @param oauthProviderName ID of third-party OAuth provider
//	//	 * @return All defined OAuth integration settings
//	//	 */
//	//	@RequestMapping(value = "/{authId}", method = { GET })
//	//	@ResponseBody
//	//	@ResponseStatus(HttpStatus.OK)
//	//	@ApiOperation(value = "Returns OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
//	//	public OAuthDetailsResource getOAuthSettings(@PathVariable String profileId, @PathVariable("authId") String oauthProviderName) {
//	//
//	//		ServerSettings settings = repository.findOne(profileId);
//	//		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);
//	//
//	//		return Optional.ofNullable(settings.getoAuth2LoginDetails())
//	//				.flatMap(details -> Optional.ofNullable(details.get(oauthProviderName)))
//	//				.map(OAuthDetailsConverters.TO_RESOURCE)
//	//				.orElseThrow(() -> new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND, oauthProviderName));
//	//
//	//	}
}
