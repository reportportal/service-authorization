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
package com.epam.reportportal.auth;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;

/**
 * Facade for {@link org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices} to simplify work with them
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Service
public class TokenServicesFacade {
	private final DefaultTokenServices jwtTokenServices;
	private final OAuth2RequestFactory oAuth2RequestFactory;
	private final ClientDetailsService clientDetailsService;

	@Autowired
	public TokenServicesFacade(DefaultTokenServices jwtTokenServices, ClientDetailsService clientDetailsService) {
		this.jwtTokenServices = jwtTokenServices;
		this.clientDetailsService = clientDetailsService;
		this.oAuth2RequestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
	}

	public OAuth2AccessToken createToken(ReportPortalClient client, String username, Authentication userAuthentication,
			Map<String, Serializable> extensionParams) {
			return createNonApiToken(client, username, userAuthentication, extensionParams);
	}

	public OAuth2AccessToken createNonApiToken(ReportPortalClient client, String username, Authentication userAuthentication,
			Map<String, Serializable> extensionParams) {
		OAuth2Request oAuth2Request = createOAuth2Request(client, username, extensionParams);
		return jwtTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, userAuthentication));
	}

	private OAuth2Request createOAuth2Request(ReportPortalClient client, String username, Map<String, Serializable> extensionParams) {
		//@formatter:off
		ClientDetails clientDetails = clientDetailsService.loadClientByClientId(client.name());
		OAuth2Request oAuth2Request = oAuth2RequestFactory.createOAuth2Request(clientDetails, oAuth2RequestFactory.createTokenRequest(
				ImmutableMap.<String, String>builder()
						.put("client_id", client.name())
						.put("username", username)
						.put("grant", "password")
						.build(), clientDetails));
		oAuth2Request.getExtensions().putAll(extensionParams);
		//@formatter:on
		return oAuth2Request;
	}
}
