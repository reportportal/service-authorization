/*
 * Copyright 2025 EPAM Systems
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
package com.epam.reportportal.auth.config.password;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class CustomCodeGrantAuthenticationProvider implements AuthenticationProvider {

  private static final String ERROR_URI = "/login";
  private final OAuth2AuthorizationService authorizationService;
  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;

  public CustomCodeGrantAuthenticationProvider(OAuth2AuthorizationService authorizationService,
      OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator, UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder) {
    Assert.notNull(authorizationService, "authorizationService cannot be null");
    Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
    this.authorizationService = authorizationService;
    this.tokenGenerator = tokenGenerator;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    CustomCodeGrantAuthenticationToken customCodeGrantAuthentication = (CustomCodeGrantAuthenticationToken) authentication;
    OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
        customCodeGrantAuthentication);
    RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
    String username = customCodeGrantAuthentication.getUsername();
    String password = customCodeGrantAuthentication.getPassword();
    Set<String> authorizedScopes = customCodeGrantAuthentication.getScope();
    UserDetails user = null;
    try {
      user = userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
      throw new AccessDeniedException("Bad credentials");
    }
    if (!passwordEncoder.matches(password, user.getPassword()) || !user.getUsername().equals(
        username)) {
      throw new AccessDeniedException("Bad credentials");
    }
    Authentication usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user, null,
        user.getAuthorities());

    DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
        .registeredClient(registeredClient)
        .principal(usernamePasswordAuthenticationToken)
        .authorizationServerContext(AuthorizationServerContextHolder.getContext())
        .authorizedScopes(authorizedScopes)
        .authorizationGrantType(customCodeGrantAuthentication.getGrantType())
        .authorizationGrant(customCodeGrantAuthentication);

    // ----- Access Token -----
    OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN)
        .principal(usernamePasswordAuthenticationToken).build();
    OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
    if (generatedAccessToken == null) {
      OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the access token.", null);
      throw new OAuth2AuthenticationException(error);
    }
    OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
        generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
        generatedAccessToken.getExpiresAt(), null);

    OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
        .principalName(clientPrincipal.getName())
        .authorizationGrantType(customCodeGrantAuthentication.getGrantType());
    if (generatedAccessToken instanceof ClaimAccessor) {
      authorizationBuilder.token(accessToken,
          (metadata) -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
              ((ClaimAccessor) generatedAccessToken).getClaims()));
    } else {
      authorizationBuilder.accessToken(accessToken);
    }

    // ----- Refresh Token -----
    OAuth2RefreshToken refreshToken = null;
    if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)
        && !clientPrincipal.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.NONE)) {
      tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
      OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
      OAuth2RefreshToken generatedRefreshToken = refreshTokenGenerator.generate(tokenContext);
      if (generatedRefreshToken == null) {
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
            "The token generator failed to generate the refresh token.", ERROR_URI);
        throw new OAuth2AuthenticationException(error);
      }
      refreshToken = generatedRefreshToken;
      authorizationBuilder.refreshToken(refreshToken);
    }

    Map<String, Object> additionalParameters = Collections.emptyMap();

    OAuth2Authorization authorization = authorizationBuilder
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .authorizedScopes(authorizedScopes)
        .attribute(Principal.class.getName(), usernamePasswordAuthenticationToken)
        .build();
    this.authorizationService.save(authorization);

    return new OAuth2AccessTokenAuthenticationToken(registeredClient, usernamePasswordAuthenticationToken, accessToken, refreshToken, additionalParameters);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return CustomCodeGrantAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
      Authentication authentication) {
    OAuth2ClientAuthenticationToken clientPrincipal = null;
    if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
      clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
    }
    if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
      return clientPrincipal;
    }
    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
  }

}
