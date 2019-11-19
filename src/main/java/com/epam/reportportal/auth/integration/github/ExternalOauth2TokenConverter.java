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
package com.epam.reportportal.auth.integration.github;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ExternalOauth2TokenConverter extends DefaultAccessTokenConverter {

	static final String UPSTREAM_TOKEN = "upstream_token";

	@Override
	public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		Map<String, Object> params = (Map<String, Object>) super.convertAccessToken(token, authentication);
		ofNullable(authentication.getOAuth2Request()).map(r -> r.getExtensions().get(UPSTREAM_TOKEN))
				.ifPresent(uToken -> params.put(UPSTREAM_TOKEN, String.valueOf(uToken)));
		return params;
	}

	@Override
	public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
		OAuth2Authentication oAuth2Authentication = super.extractAuthentication(map);
		ofNullable(oAuth2Authentication.getOAuth2Request()).map(OAuth2Request::getExtensions)
				.ifPresent(extensions -> ofNullable(map.get(UPSTREAM_TOKEN)).map(String::valueOf)
						.ifPresent(token -> extensions.put(UPSTREAM_TOKEN, token)));
		return oAuth2Authentication;
	}
}
