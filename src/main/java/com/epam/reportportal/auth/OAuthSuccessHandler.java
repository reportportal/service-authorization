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

import com.epam.reportportal.auth.event.UiUserSignedInEvent;
import com.epam.ta.reportportal.commons.validation.Suppliers;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
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

import static java.util.Optional.ofNullable;

/**
 * Success handler for external oauth. Generates internal token for authenticated user to be used on UI/Agents side
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private static final String LOGIN_ATTRIBUTE = "login";

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
		OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
		String login = ofNullable(oauth.getPrincipal().getAttributes().get(LOGIN_ATTRIBUTE)).map(String::valueOf)
				.orElseThrow(() -> new ReportPortalException(ErrorType.ACCESS_DENIED,
						Suppliers.formattedSupplier("Attribute - {} was not provided.", LOGIN_ATTRIBUTE).get()
				));
		OAuth2AccessToken accessToken = tokenServicesFacade.get().createToken(ReportPortalClient.ui, login, oauth, false);

		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("token", accessToken.getValue());
		query.add("token_type", accessToken.getTokenType());
		//TODO replace hard-coded port with custom value
		URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
				.port(8080)
				.replacePath("/ui/auth_success")
				.replaceQueryParams(query)
				.build()
				.toUri();

		eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

		getRedirectStrategy().sendRedirect(request, response, rqUrl.toString());
	}
}
