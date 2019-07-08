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

import com.epam.reportportal.auth.event.UiUserSignedInEvent;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static java.util.Optional.ofNullable;

/**
 * Success handler for external oauth. Generates internal token for authenticated user to be used on UI/Agents side
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	/*
	 * Internal token services facade
	 */
	@Autowired
	private Provider<TokenServicesFacade> tokenServicesFacade;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	OAuthSuccessHandler() {
		super("/");
	}

	@Override
	protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		OAuth2Authentication oAuth2Authentication = ofNullable((OAuth2Authentication) authentication).orElseThrow(() -> new ReportPortalException(
				ErrorType.ACCESS_DENIED));
		String login = String.valueOf(oAuth2Authentication.getPrincipal());
		OAuth2AccessToken accessToken = tokenServicesFacade.get()
				.createToken(ReportPortalClient.ui,
						normalizeId(login),
						authentication,
						ofNullable(oAuth2Authentication.getOAuth2Request()).map(OAuth2Request::getExtensions)
								.orElseGet(Collections::emptyMap)
				);

		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("token", accessToken.getValue());
		query.add("token_type", accessToken.getTokenType());
		URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.replacePath("/ui/authSuccess")
				.replaceQueryParams(query)
				.build()
				.toUri();

		eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

		getRedirectStrategy().sendRedirect(request, response, rqUrl.toString().replaceFirst("/ui/authSuccess", "/#authSuccess"));
	}
}
