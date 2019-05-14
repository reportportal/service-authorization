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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import javax.inject.Provider;

/**
 * Success handler for external oauth. Generates internal token for authenticated user to be used on UI/Agents side
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class OAuthSuccessHandler extends AuthenticationSuccessHandler {

	@Autowired
	public OAuthSuccessHandler(Provider<TokenServicesFacade> tokenFacadeProvider, ApplicationEventPublisher eventPublisher) {
		super(tokenFacadeProvider, eventPublisher);
	}

	@Override
	protected OAuth2AccessToken getToken(Authentication authentication) {
		OAuth2Authentication oauth = (OAuth2Authentication) authentication;
		return tokenServicesFacade.get()
				.createToken(ReportPortalClient.ui, oauth.getName(), oauth.getUserAuthentication(), oauth.getOAuth2Request().getExtensions());
	}
}
