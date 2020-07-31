/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Base SSO controller
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@RestController
@Transactional
public class SsoEndpoint {

	private final TokenServicesFacade tokenServicesFacade;

	@Autowired
	public SsoEndpoint(TokenServicesFacade tokenServicesFacade) {
		this.tokenServicesFacade = tokenServicesFacade;
	}

	@RequestMapping(value = { "/sso/me", "/sso/user" }, method = { GET, POST })
	public Map<String, Object> user(Authentication user) {

		ImmutableMap.Builder<String, Object> details = ImmutableMap.<String, Object>builder().put("user", user.getName())
				.put("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

		if (user.getPrincipal() instanceof ReportPortalUser) {
			details.put("userId", ((ReportPortalUser) user.getPrincipal()).getUserId());
			details.put("projects", ((ReportPortalUser) user.getPrincipal()).getProjectDetails());
		}
		return details.build();
	}

	@RequestMapping(value = { "/sso/me/apitoken" }, method = GET)
	@ApiOperation(value = "Get api token")
	public OAuth2AccessToken getApiToken(Principal user) {
		Optional<OAuth2AccessToken> tokens = tokenServicesFacade.getTokens(user.getName(), ReportPortalClient.api).findAny();
		BusinessRule.expect(tokens, Optional::isPresent).verify(ErrorType.USER_NOT_FOUND, user.getName());
		return tokens.get();
	}

	@RequestMapping(value = { "/sso/me/apitoken" }, method = POST)
	@ApiOperation(value = "Create api token")
	public OAuth2AccessToken createApiToken(OAuth2Authentication user) {
		tokenServicesFacade.revokeUserTokens(user.getName(), ReportPortalClient.api);
		return tokenServicesFacade.createToken(ReportPortalClient.api,
				user.getName(),
				user.getUserAuthentication(),
				Collections.emptyMap()
		);
	}

}