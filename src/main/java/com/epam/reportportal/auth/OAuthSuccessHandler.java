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

import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import java.util.Collections;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static java.util.Optional.ofNullable;

/**
 * Success handler for external oauth. Generates internal token for authenticated user to be used on UI/Agents side
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class OAuthSuccessHandler extends AuthSuccessHandler {

	@Autowired
	public OAuthSuccessHandler(Provider<TokenServicesFacade> tokenServicesFacade, ApplicationEventPublisher eventPublisher) {
		super(tokenServicesFacade, eventPublisher);
	}

	@Override
	protected OAuth2AccessToken getToken(Authentication authentication) {
		OAuth2Authentication oAuth2Authentication = ofNullable((OAuth2Authentication) authentication).orElseThrow(() -> new ReportPortalException(
				ErrorType.ACCESS_DENIED));
		String login = String.valueOf(oAuth2Authentication.getPrincipal());
		return tokenServicesFacade.get().createToken(
				ReportPortalClient.ui,
				normalizeId(login),
				authentication,
				ofNullable(oAuth2Authentication.getOAuth2Request()).map(OAuth2Request::getExtensions).orElseGet(Collections::emptyMap)
		);
	}
}
