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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Base class for handling of success authentication and redirection to UI page.
 *
 * @author Yevgeniy Svalukhin
 */
public abstract class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	protected Provider<TokenServicesFacade> tokenServicesFacade;

	protected ApplicationEventPublisher eventPublisher;

	protected AuthorizationServerTokenServices tokenServices;

	public AuthSuccessHandler(Provider<TokenServicesFacade> tokenServicesFacade, ApplicationEventPublisher eventPublisher,
			@Qualifier("externalOauth2TokenServices") AuthorizationServerTokenServices tokenServices) {
		super("/");
		this.tokenServicesFacade = tokenServicesFacade;
		this.eventPublisher = eventPublisher;
		this.tokenServices = tokenServices;
	}

	@Override
	protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		OAuth2AccessToken token = getToken(authentication);

		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("token", token.getValue());
		query.add("token_type", token.getTokenType());
		URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.replacePath("/ui/authSuccess")
				.replaceQueryParams(query)
				.build()
				.toUri();

		eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

		getRedirectStrategy().sendRedirect(request, response, rqUrl.toString().replaceFirst("/ui/authSuccess", "/#authSuccess"));
	}

	protected abstract OAuth2AccessToken getToken(Authentication authentication);
}
