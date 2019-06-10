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
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.stream.Collectors;

import static com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters.FROM_SPRING;
import static java.util.Optional.ofNullable;

public class OAuthProviderFactory {
	public static OAuthRegistration fillOAuthRegistration(String oauthProviderId, OAuthRegistration registration) {
		ClientRegistration springRegistration;
		switch (oauthProviderId) {
			case "github":
				springRegistration = createGitHubProvider(oauthProviderId, registration);
				break;
			default:
				throw new ReportPortalException("Unsupported OAuth provider.");
		}
		OAuthRegistration filledRegistration = FROM_SPRING.apply(springRegistration);
		ofNullable(registration.getRestrictions()).ifPresent(restrictions -> filledRegistration.setRestrictions(restrictions.stream()
				.peek(r -> r.setRegistration(filledRegistration))
				.collect(Collectors.toSet())));
		return filledRegistration;
	}

	private static ClientRegistration createGitHubProvider(String oauthProviderId, OAuthRegistration registration) {
		return CommonOAuth2Provider.GITHUB.getBuilder(oauthProviderId)
				.clientId(registration.getClientId())
				.clientSecret(registration.getClientSecret())
				.scope("read:user", "user:email")
				.clientName(oauthProviderId)
				.build();
	}
}
