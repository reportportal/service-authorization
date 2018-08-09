package com.epam.reportportal.auth.oauth;

import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import static com.epam.reportportal.auth.converter.OAuthRegistrationConverters.FROM_SPRING;

public class OAuthProviderFactory {
	public static OAuthRegistration fillOAuthRegistration(OAuthRegistration registration) {
		ClientRegistration springRegistration;
		switch (registration.getId()) {
			case "github":
				springRegistration = createGitHubProvider(registration);
				break;
			default:
				throw new ReportPortalException("Unsupported OAuth provider.");
		}
		OAuthRegistration filledRegistration = FROM_SPRING.apply(springRegistration);
		filledRegistration.setRestrictions(registration.getRestrictions());
		return filledRegistration;
	}

	private static ClientRegistration createGitHubProvider(OAuthRegistration registration) {
		return CommonOAuth2Provider.GITHUB.getBuilder(registration.getId())
				.clientId(registration.getClientId())
				.clientSecret(registration.getClientSecret())
				.scope("read:user", "user:email")
				.clientName(registration.getId())
				.build();
	}
}
