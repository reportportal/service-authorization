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
package com.epam.reportportal.auth;

import com.epam.reportportal.auth.converter.OAuthDetailsConverters;
import com.epam.reportportal.auth.oauth.OAuthProvider;
import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.settings.OAuthDetailsResource;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.Predicates.isPresent;
import static java.util.stream.Collectors.toMap;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Endpoint for oauth configs
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Controller
@RequestMapping("/settings/{profileId}/oauth")
public class OAuthConfigurationEndpoint {

	private final ServerSettingsRepository repository;
	private final Map<String, OAuthProvider> providers;

	@Autowired
	public OAuthConfigurationEndpoint(ServerSettingsRepository repository, Map<String, OAuthProvider> providers) {
		this.repository = repository;
		this.providers = providers;
	}

	/**
	 * Updates oauth integration settings
	 *
	 * @param profileId         settings ProfileID
	 * @param oauthDetails      OAuth details resource update
	 * @param oauthProviderName ID of third-party OAuth provider
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/{authId}", method = { POST, PUT })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Updates ThirdParty OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
	public Map<String, OAuthDetailsResource> updateOAuthSettings(@PathVariable String profileId,
			@PathVariable("authId") String oauthProviderName, @RequestBody @Validated OAuthDetailsResource oauthDetails) {

		Optional<OAuthProvider> oAuthProvider = Optional.ofNullable(providers.get(oauthProviderName));
		BusinessRule.expect(oAuthProvider, isPresent()).verify(ErrorType.OAUTH_INTEGRATION_NOT_FOUND, profileId);

		ServerSettings settings = repository.findOne(profileId);
		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);
		Map<String, OAuth2LoginDetails> serverOAuthDetails = Optional.ofNullable(settings.getoAuth2LoginDetails()).orElse(new HashMap<>());

		OAuth2LoginDetails loginDetails = OAuthDetailsConverters.FROM_RESOURCE.apply(oauthDetails);
		oAuthProvider.get().applyDefaults(loginDetails);
		serverOAuthDetails.put(oauthProviderName, loginDetails);

		settings.setoAuth2LoginDetails(serverOAuthDetails);

		repository.save(settings);
		return serverOAuthDetails.entrySet().stream()
				.collect(toMap(Map.Entry::getKey, e -> OAuthDetailsConverters.TO_RESOURCE.apply(e.getValue())));
	}

	/**
	 * Deletes oauth integration settings
	 *
	 * @param profileId         settings ProfileID
	 * @param oauthProviderName ID of third-party OAuth provider
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/{authId}", method = { DELETE })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Deletes ThirdParty OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
	public OperationCompletionRS deleteOAuthSetting(@PathVariable String profileId, @PathVariable("authId") String oauthProviderName) {

		ServerSettings settings = repository.findOne(profileId);
		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);
		Map<String, OAuth2LoginDetails> serverOAuthDetails = Optional.of(settings.getoAuth2LoginDetails()).orElse(new HashMap<>());

		if (null != serverOAuthDetails.remove(oauthProviderName)) {
			settings.setoAuth2LoginDetails(serverOAuthDetails);
			repository.save(settings);
		} else {
			throw new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND);
		}

		return new OperationCompletionRS("Auth settings '" + oauthProviderName + "' were successfully removed");
	}

	/**
	 * Returns oauth integration settings
	 *
	 * @param profileId settings ProfileID
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/", method = { GET })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Returns OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
	public Map<String, OAuthDetailsResource> getOAuthSettings(@PathVariable String profileId) {

		ServerSettings settings = repository.findOne(profileId);
		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);

		return Optional.ofNullable(settings.getoAuth2LoginDetails()).map(details -> details.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> OAuthDetailsConverters.TO_RESOURCE.apply(e.getValue()))))
				.orElseThrow(() -> new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND));

	}

	/**
	 * Returns oauth integration settings
	 *
	 * @param profileId         settings ProfileID
	 * @param oauthProviderName ID of third-party OAuth provider
	 * @return All defined OAuth integration settings
	 */
	@RequestMapping(value = "/{authId}", method = { GET })
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Returns OAuth Server Settings", notes = "'default' profile is using till additional UI implementations")
	public OAuthDetailsResource getOAuthSettings(@PathVariable String profileId, @PathVariable("authId") String oauthProviderName) {

		ServerSettings settings = repository.findOne(profileId);
		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);

		return Optional.ofNullable(settings.getoAuth2LoginDetails()).flatMap(details -> Optional.ofNullable(details.get(oauthProviderName)))
				.map(OAuthDetailsConverters.TO_RESOURCE)
				.orElseThrow(() -> new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND, oauthProviderName));

	}
}
