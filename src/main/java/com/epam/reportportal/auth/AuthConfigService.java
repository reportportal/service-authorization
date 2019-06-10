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

import com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.reflect.Reflection.newProxy;
import static java.util.Optional.ofNullable;

/**
 * Builds proxy instance of {@link RestTemplate} which load OAuth resouce details from DB on each operation
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class AuthConfigService {

	@Autowired
	private MutableClientRegistrationRepository clientRegistrationRepository;

	/**
	 * Builds proxy instance of {@link RestTemplate} which load OAuth resouce details from DB on each operation
	 *
	 * @param name                Name/ID of resource of {@link RestTemplate}
	 * @param oauth2ClientContext OAuth Client context
	 * @return Proxy instance of {@link RestTemplate}
	 */
	public OAuth2RestOperations getRestTemplate(String name, OAuth2ClientContext oauth2ClientContext) {
		return newProxy(OAuth2RestOperations.class, (proxy, method, args) -> {
			try {
				return method.invoke(new OAuth2RestTemplate(loadResourceDetails(name), oauth2ClientContext), args);

			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		});
	}

	public Supplier<OAuthRegistrationResource> getLoginDetailsSupplier(String name) {
		return () -> loadOAuthRegistration(name).orElseThrow(() -> noAuthDetailsException(name));
	}

	/**
	 * Loads {@link OAuthRegistration} from database and converts it to the {@link OAuthRegistrationResource}
	 *
	 * @param id {@link OAuthRegistration#id}
	 * @return Built {@link OAuthRegistrationResource}
	 */
	public Optional<OAuthRegistrationResource> loadOAuthRegistration(String id) {
		return clientRegistrationRepository.findOAuthRegistrationById(id).map(OAuthRegistrationConverters.TO_RESOURCE);
	}

	/**
	 * Loads {@link OAuth2ProtectedResourceDetails} from database
	 *
	 * @param name Name of resource
	 * @return Built {@link OAuth2ProtectedResourceDetails}
	 */
	public OAuth2ProtectedResourceDetails loadResourceDetails(String name) {
		return loadOAuthRegistration(name).map(RESOURCE_DETAILS_CONVERTER).orElseThrow(() -> noAuthDetailsException(name));
	}

	/**
	 * Converts DB model to {@link OAuth2ProtectedResourceDetails}
	 */
	private static final Function<OAuthRegistrationResource, OAuth2ProtectedResourceDetails> RESOURCE_DETAILS_CONVERTER = d -> {
		BaseOAuth2ProtectedResourceDetails details = getOauth2ProtectedResourceDetails(d);
		details.setId(d.getId());
		details.setAccessTokenUri(d.getTokenUri());
		Arrays.stream(AuthenticationScheme.values())
				.filter(scheme -> scheme.name().equalsIgnoreCase(d.getClientAuthMethod()))
				.findFirst()
				.ifPresent(details::setClientAuthenticationScheme);
		details.setClientId(d.getClientId());
		details.setClientSecret(d.getClientSecret());
		details.setScope(new ArrayList<>(d.getScopes()));
		return details;
	};

	private static BaseOAuth2ProtectedResourceDetails getOauth2ProtectedResourceDetails(
			OAuthRegistrationResource oAuthRegistrationResource) {
		return ofNullable(oAuthRegistrationResource.getAuthGrantType()).map(grantType -> {
			Optional<String> authorizationUri = ofNullable(oAuthRegistrationResource.getAuthorizationUri());
			switch (grantType) {
				case "authorization_code":
					AuthorizationCodeResourceDetails authorizationCodeResourceDetails = new AuthorizationCodeResourceDetails();
					authorizationUri.ifPresent(authorizationCodeResourceDetails::setUserAuthorizationUri);
					return authorizationCodeResourceDetails;
				case "implicit":
					ImplicitResourceDetails implicitResourceDetails = new ImplicitResourceDetails();
					authorizationUri.ifPresent(implicitResourceDetails::setUserAuthorizationUri);
					return implicitResourceDetails;
				case "client_credentials":
					return new ClientCredentialsResourceDetails();
				case "password":
					return new ResourceOwnerPasswordResourceDetails();
				default:
					return new BaseOAuth2ProtectedResourceDetails();
			}
		}).orElseGet(BaseOAuth2ProtectedResourceDetails::new);

	}

	private ProviderNotFoundException noAuthDetailsException(String name) {
		return new ProviderNotFoundException("Auth details '" + name + "' are not configured");
	}
}
