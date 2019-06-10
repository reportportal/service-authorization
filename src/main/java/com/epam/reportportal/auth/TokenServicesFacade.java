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

//import org.springframework.security.oauth2.provider.*;
//import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import com.epam.ta.reportportal.dao.OAuth2AccessTokenRepository;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Facade for {@link org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices} to simplify work with them
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Service
public class TokenServicesFacade {

	private final DefaultTokenServices databaseTokenServices;
	private final DefaultTokenServices jwtTokenServices;
	private final OAuth2RequestFactory oAuth2RequestFactory;
	private final ClientDetailsService clientDetailsService;
	private final OAuth2AccessTokenRepository tokenRepository;

	@Autowired
	public TokenServicesFacade(@Qualifier(value = "databaseTokenServices") DefaultTokenServices databaseTokenServices,
			DefaultTokenServices jwtTokenServices, ClientDetailsService clientDetailsService, OAuth2AccessTokenRepository tokenRepository) {
		this.databaseTokenServices = databaseTokenServices;
		this.jwtTokenServices = jwtTokenServices;
		this.clientDetailsService = clientDetailsService;
		this.oAuth2RequestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
		this.tokenRepository = tokenRepository;
	}

	public Stream<OAuth2AccessToken> getTokens(String username, ReportPortalClient client) {
		return tokenRepository.findByClientIdAndUserName(client.name(), username)
				.map(token -> SerializationUtils.deserialize(token.getToken()));
	}

	public OAuth2AccessToken createToken(ReportPortalClient client, String username, Authentication userAuthentication,
			Map<String, Serializable> extensionParams) {
		if (client == ReportPortalClient.api) {
			return createApiToken(client, username, userAuthentication, extensionParams);
		} else {
			return createNonApiToken(client, username, userAuthentication, extensionParams);
		}

	}

	public OAuth2AccessToken createToken(AuthorizationServerTokenServices externalTokenServices, ReportPortalClient client, String username,
			Authentication userAuthentication, Map<String, Serializable> extensionParams) {
		OAuth2Request oAuth2Request = createOAuth2Request(client, username, extensionParams);
		return externalTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, userAuthentication));
	}

	public OAuth2AccessToken createApiToken(ReportPortalClient client, String username, Authentication userAuthentication,
			Map<String, Serializable> extensionParams) {
		OAuth2Request oAuth2Request = createOAuth2Request(client, username, extensionParams);
		return databaseTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, userAuthentication));
	}

	public OAuth2AccessToken createNonApiToken(ReportPortalClient client, String username, Authentication userAuthentication,
			Map<String, Serializable> extensionParams) {
		OAuth2Request oAuth2Request = createOAuth2Request(client, username, extensionParams);
		return jwtTokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, userAuthentication));
	}

	public OAuth2AccessToken getAccessToken(Authentication userAuthentication) {
		return databaseTokenServices.getAccessToken((OAuth2Authentication) userAuthentication);
	}

	public void revokeUserTokens(String user, ReportPortalClient client) {
		this.tokenRepository.findByClientIdAndUserName(client.name(), user)
				.forEach(token -> databaseTokenServices.revokeToken(token.getTokenId()));
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
