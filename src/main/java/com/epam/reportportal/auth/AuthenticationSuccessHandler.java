/*
 * Copyright 2019 EPAM Systems
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
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
public abstract class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    protected Provider<TokenServicesFacade> tokenServicesFacade;

    protected ApplicationEventPublisher eventPublisher;

    public AuthenticationSuccessHandler(Provider<TokenServicesFacade> tokenFacadeProvider,
                                        ApplicationEventPublisher eventPublisher) {
        super("/");
        this.tokenServicesFacade = tokenFacadeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2AccessToken accessToken = getToken(authentication);

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("token", accessToken.getValue());
        query.add("token_type", accessToken.getTokenType());
        URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).replacePath("/ui/authSuccess.html")
                .replaceQueryParams(query).build().toUri();

        eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

        getRedirectStrategy().sendRedirect(request, response, rqUrl.toString());
    }

    protected abstract OAuth2AccessToken getToken(Authentication authentication);
}
