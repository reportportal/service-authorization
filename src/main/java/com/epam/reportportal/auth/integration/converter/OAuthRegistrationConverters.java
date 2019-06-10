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
package com.epam.reportportal.auth.integration.converter;

import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistrationRestriction;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistrationScope;
import com.epam.ta.reportportal.exception.DataStorageException;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import com.google.common.base.Preconditions;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Converter between resource, database, default Spring representation of OAuthRegistration.
 *
 * @author Anton Machulski
 */
public class OAuthRegistrationConverters {
	private OAuthRegistrationConverters() {
		//static only
	}

	public static final Collector<OAuthRegistrationResource, ?, Map<String, OAuthRegistrationResource>> RESOURCE_KEY_MAPPER = Collectors.toMap(OAuthRegistrationResource::getId,
			r -> r
	);

	public static final Function<String, OAuthRegistrationScope> SCOPE_FROM_RESOURCE = s -> {
		OAuthRegistrationScope oAuthRegistrationScope = new OAuthRegistrationScope();
		oAuthRegistrationScope.setScope(s);
		return oAuthRegistrationScope;
	};

	public final static Function<OAuthRegistrationResource, OAuthRegistration> FROM_RESOURCE = resource -> {
		Preconditions.checkNotNull(resource);
		OAuthRegistration db = new OAuthRegistration();
		db.setId(resource.getId());
		db.setClientId(resource.getClientId());
		db.setClientSecret(resource.getClientSecret());
		db.setClientAuthMethod(resource.getClientAuthMethod());
		db.setAuthGrantType(resource.getAuthGrantType());
		db.setRedirectUrlTemplate(resource.getRedirectUrlTemplate());
		db.setAuthorizationUri(resource.getAuthorizationUri());
		db.setTokenUri(resource.getTokenUri());
		db.setUserInfoEndpointUri(resource.getUserInfoEndpointUri());
		db.setUserInfoEndpointNameAttribute(resource.getUserInfoEndpointNameAttribute());
		db.setJwkSetUri(resource.getJwkSetUri());
		db.setClientName(resource.getClientName());
		db.setScopes(ofNullable(resource.getScopes()).map(scopes -> scopes.stream()
				.map(SCOPE_FROM_RESOURCE)
				.peek(scope -> scope.setRegistration(db))
				.collect(Collectors.toSet())).orElse(Collections.emptySet()));
		List<OAuthRegistrationRestriction> restrictions = OAuthRestrictionConverter.FROM_RESOURCE.apply(resource);
		db.setRestrictions(restrictions.stream().peek(restriction -> restriction.setRegistration(db)).collect(Collectors.toSet()));
		return db;
	};

	public final static Function<OAuthRegistration, OAuthRegistrationResource> TO_RESOURCE = db -> {
		Preconditions.checkNotNull(db);
		OAuthRegistrationResource resource = new OAuthRegistrationResource();
		resource.setId(db.getId());
		resource.setClientId(db.getClientId());
		resource.setClientSecret(db.getClientSecret());
		resource.setClientAuthMethod(db.getClientAuthMethod());
		resource.setAuthGrantType(db.getAuthGrantType());
		resource.setRedirectUrlTemplate(db.getRedirectUrlTemplate());
		resource.setAuthorizationUri(db.getAuthorizationUri());
		resource.setTokenUri(db.getTokenUri());
		resource.setUserInfoEndpointUri(db.getUserInfoEndpointUri());
		resource.setUserInfoEndpointNameAttribute(db.getUserInfoEndpointNameAttribute());
		resource.setJwkSetUri(db.getJwkSetUri());
		resource.setClientName(db.getClientName());
		ofNullable(db.getScopes()).ifPresent(scopes -> resource.setScopes(scopes.stream()
				.map(OAuthRegistrationScope::getScope)
				.collect(Collectors.toSet())));
		ofNullable(db.getRestrictions()).ifPresent(r -> resource.setRestrictions(OAuthRestrictionConverter.TO_RESOURCE.apply(db)));
		return resource;
	};

	public static final Function<OAuthRegistration, ClientRegistration> TO_SPRING = registration -> ClientRegistration.withRegistrationId(
			registration.getClientName())
			.clientId(registration.getClientId())
			.clientSecret(registration.getClientSecret())
			.clientAuthenticationMethod(new ClientAuthenticationMethod(registration.getClientAuthMethod()))
			.authorizationGrantType(new AuthorizationGrantType(registration.getAuthGrantType()))
			.redirectUriTemplate(registration.getRedirectUrlTemplate())
			.authorizationUri(registration.getAuthorizationUri())
			.tokenUri(registration.getTokenUri())
			.userInfoUri(registration.getUserInfoEndpointUri())
			.userNameAttributeName(registration.getUserInfoEndpointNameAttribute())
			.jwkSetUri(registration.getJwkSetUri())
			.clientName(registration.getClientName())
			.scope(ofNullable(registration.getScopes()).orElseThrow(() -> new DataStorageException(
					"Inconsistent data. Scopes for clientRegistration not provided."))
					.stream()
					.map(OAuthRegistrationScope::getScope)
					.toArray(String[]::new))
			.build();

	public static final Function<ClientRegistration, OAuthRegistration> FROM_SPRING = fullRegistration -> {
		OAuthRegistration registration = new OAuthRegistration();
		registration.setId(fullRegistration.getRegistrationId());
		registration.setClientId(fullRegistration.getClientId());
		registration.setClientSecret(fullRegistration.getClientSecret());
		registration.setClientAuthMethod(fullRegistration.getClientAuthenticationMethod().getValue());
		registration.setAuthGrantType(fullRegistration.getAuthorizationGrantType().getValue());
		registration.setRedirectUrlTemplate(fullRegistration.getRedirectUriTemplate());
		registration.setScopes(fullRegistration.getScopes().stream().map(scope -> {
			OAuthRegistrationScope scopeNew = new OAuthRegistrationScope();
			scopeNew.setRegistration(registration);
			scopeNew.setScope(scope);
			return scopeNew;
		}).collect(Collectors.toSet()));
		ClientRegistration.ProviderDetails details = fullRegistration.getProviderDetails();
		registration.setAuthorizationUri(details.getAuthorizationUri());
		registration.setTokenUri(details.getTokenUri());
		registration.setUserInfoEndpointUri(details.getUserInfoEndpoint().getUri());
		registration.setUserInfoEndpointNameAttribute(details.getUserInfoEndpoint().getUserNameAttributeName());
		registration.setJwkSetUri(details.getJwkSetUri());
		registration.setClientName(fullRegistration.getClientName());
		return registration;
	};
}
