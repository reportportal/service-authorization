package com.epam.reportportal.auth;

import com.epam.reportportal.auth.converter.OAuthDetailsConverters;
import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.ServerSettings;
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

import static java.util.stream.Collectors.toMap;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

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
		if (OAuthSecurityConfig.GITHUB.equals(oauthProviderName)) {
			oauthDetails.setScope(Collections.singletonList("user"));
			oauthDetails.setGrantType("authorization_code");
			oauthDetails.setAccessTokenUri("https://github.com/login/oauth/access_token");
			oauthDetails.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
			oauthDetails.setClientAuthenticationScheme("form");
		}
		serverOAuthDetails.put(oauthProviderName, OAuthDetailsConverters.FROM_RESOURCE.apply(oauthDetails));
		settings.setoAuth2LoginDetails(serverOAuthDetails);

		repository.save(settings);
		return serverOAuthDetails.entrySet().stream()
				.collect(toMap(Map.Entry::getKey, e -> OAuthDetailsConverters.TO_RESOURCE.apply(e.getValue())));
	}
}
