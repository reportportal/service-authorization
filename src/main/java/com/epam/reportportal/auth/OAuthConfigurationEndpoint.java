package com.epam.reportportal.auth;

import com.epam.reportportal.auth.converter.OAuthDetailsConverters;
import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.ServerSettings;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.settings.OAuthDetailsResource;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @author Andrei Varabyeu
 */
@Controller
@RequestMapping("/settings/{profileId}/oauth")
public class OAuthConfigurationEndpoint {

	@Autowired
	private ServerSettingsRepository repository;

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

		ServerSettings settings = repository.findOne(profileId);
		BusinessRule.expect(settings, Predicates.notNull()).verify(ErrorType.SERVER_SETTINGS_NOT_FOUND, profileId);
		Map<String, OAuth2LoginDetails> serverOAuthDetails = Optional.of(settings.getoAuth2LoginDetails()).orElse(new HashMap<>());

		OAuth2LoginDetails loginDetails = OAuthDetailsConverters.FROM_RESOURCE.apply(oauthDetails);
		if (null != loginDetails.getClientId()) {
			serverOAuthDetails.put(oauthProviderName, loginDetails);
			if (OAuthSecurityConfig.GITHUB.equals(oauthProviderName)) {
				oauthDetails.setScope(Collections.singletonList("user"));
				oauthDetails.setGrantType("authorization_code");
				oauthDetails.setAccessTokenUri("https://github.com/login/oauth/access_token");
				oauthDetails.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
				oauthDetails.setClientAuthenticationScheme("form");
			}
		} else {
			serverOAuthDetails.remove(oauthProviderName);
		}

		settings.setoAuth2LoginDetails(serverOAuthDetails);

		repository.save(settings);
		return serverOAuthDetails.entrySet().stream()
				.collect(toMap(Map.Entry::getKey, e -> OAuthDetailsConverters.TO_RESOURCE.apply(e.getValue())));
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
