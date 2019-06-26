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
package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.AuthenticationSuccessHandler;
import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Used for handling successful authentication in SAML process
 *
 * @author Yevgeniy Svalukhin
 */
@Component
public class SamlAuthenticationSuccessHandler extends AuthenticationSuccessHandler {

    public SamlAuthenticationSuccessHandler(Provider<TokenServicesFacade> tokenFacadeProvider,
                                            ApplicationEventPublisher eventPublisher) {
        super(tokenFacadeProvider, eventPublisher);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected OAuth2AccessToken getToken(Authentication authentication) {
        ReportPortalSamlAuthentication samlAuthentication = (ReportPortalSamlAuthentication) authentication;
        return tokenServicesFacade.get()
                .createToken(ReportPortalClient.ui, samlAuthentication.getName(), samlAuthentication, Collections.emptyMap());
    }
}
