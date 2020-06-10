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

package com.epam.reportportal.auth.oauth;

import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import static com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters.FROM_SPRING_MERGE;

public class OAuthProviderFactory {
	public static OAuthRegistration fillOAuthRegistration(String oauthProviderId, OAuthRegistrationResource registrationResource) {

		switch (oauthProviderId) {
			case "github":
				ClientRegistration springRegistration = createGitHubProvider(oauthProviderId, registrationResource);
				return FROM_SPRING_MERGE.apply(registrationResource, springRegistration);
			default:
				throw new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND, oauthProviderId);
		}

	}

	private static ClientRegistration createGitHubProvider(String oauthProviderId, OAuthRegistrationResource registrationResource) {
		return CommonOAuth2Provider.GITHUB.getBuilder(oauthProviderId)
				.clientId(registrationResource.getClientId())
				.clientSecret(registrationResource.getClientSecret())
				.scope("read:user", "user:email", "read:org")
				.clientName(oauthProviderId)
				.build();
	}
}
