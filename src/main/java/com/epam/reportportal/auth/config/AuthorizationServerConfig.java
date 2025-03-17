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
package com.epam.reportportal.auth.config;

import com.epam.reportportal.auth.OAuthSuccessHandler;
import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import com.epam.reportportal.auth.basic.BasicPasswordAuthenticationProvider;
import com.epam.reportportal.auth.basic.DatabaseUserDetailsService;
import com.epam.reportportal.auth.config.password.CustomCodeGrantAuthenticationConverter;
import com.epam.reportportal.auth.config.password.OAuth2ErrorResponseHandler;
import com.epam.reportportal.auth.config.utils.JwtReportPortalUserConverter;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.ServerSettingsRepository;
import com.epam.reportportal.auth.entity.ServerSettings;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.github.GitHubOAuth2UserService;
import com.epam.reportportal.auth.integration.github.GitHubUserReplicator;
import com.epam.reportportal.auth.integration.ldap.ActiveDirectoryAuthProvider;
import com.epam.reportportal.auth.integration.ldap.DetailsContextMapper;
import com.epam.reportportal.auth.integration.ldap.LdapAuthProvider;
import com.epam.reportportal.auth.integration.ldap.LdapUserReplicator;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.model.settings.OAuthRegistrationResource;
import com.epam.reportportal.auth.oauth.OAuthProvider;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {

  private static final String SECRET_KEY = "secret.key";

  @Value("${rp.jwt.signing-key}")
  private String signingKey;

  @Value("${rp.jwt.token.validity-period}")
  private Integer tokenValidity;

  private final  ServerSettingsRepository serverSettingsRepository;

  private final  UserDetailsService userDetailsService;

  private final  IntegrationRepository authConfigRepository;

  private final  LdapUserReplicator ldapUserReplicator;

  private final  ApplicationEventPublisher eventPublisher;

  private final  MutableClientRegistrationRepository clientRegistrationRepository;

  private final  AuthenticationFailureHandler authenticationFailureHandler;

  private final  GitHubUserReplicator gitHubUserReplicator;

  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient uiClient = RegisteredClient.withId(ReportPortalClient.ui.name())
        .clientId(ReportPortalClient.ui.name())
        .clientSecret(passwordEncoder().encode("uiman"))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.PASSWORD)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .scope("ui")
        .tokenSettings(tokenSettings())
        .build();

    RegisteredClient apiClient = RegisteredClient.withId(ReportPortalClient.api.name())
        .clientId(ReportPortalClient.api.name())
        .clientSecret("apiman")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.PASSWORD)
        .scope("api")
        .tokenSettings(TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofDays(1))
            .build())
        .build();

    RegisteredClient internalClient = RegisteredClient.withId(ReportPortalClient.internal.name())
        .clientId(ReportPortalClient.internal.name())
        .clientSecret("internal_man")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope("internal")
        .clientSettings(ClientSettings.builder()
            .requireAuthorizationConsent(false)
            .build())
        .build();

    return new InMemoryRegisteredClientRepository(uiClient, apiClient, internalClient);
  }

  private TokenSettings tokenSettings() {
    return TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofSeconds(tokenValidity))
        .build();
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .tokenEndpoint("/sso/oauth/token")
        .tokenIntrospectionEndpoint("/sso/oauth/check_token")
        .authorizationEndpoint("/sso/oauth/authorize")
        .build();
  }
  @Bean
  public JwtEncoder jwtEncoder() {
    SecretKey key = new SecretKeySpec(getSecret().getBytes(),
        "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(getSecret().getBytes(), "HmacSHA256")).build();
  }

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer configurer =
        new OAuth2AuthorizationServerConfigurer();

    http
        .securityMatcher("/sso/oauth/token")
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .csrf().disable()
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new OAuth2ErrorResponseHandler())
            .accessDeniedHandler(new OAuth2ErrorResponseHandler())
        )
        .apply(configurer).tokenEndpoint(
            tokenEndpoint ->
                tokenEndpoint
                    .accessTokenRequestConverter(new CustomCodeGrantAuthenticationConverter())
                    .authenticationProvider(basicPasswordAuthProvider())
                    .authenticationProvider(ldapAuthProvider())
                    .authenticationProvider(activeDirectoryAuthProvider())
        );

    return http.build();
  }

  @Bean
  public AuthenticationProvider basicPasswordAuthProvider() {
    BasicPasswordAuthenticationProvider provider = new BasicPasswordAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationProvider ldapAuthProvider() {
    return new LdapAuthProvider(authConfigRepository, eventPublisher, ldapDetailsContextMapper(), new TokenServicesFacade(jwtEncoder()));
  }

  @Bean("ldapDetailsContextMapper")
  public DetailsContextMapper ldapDetailsContextMapper() {
    return new DetailsContextMapper(
        ldapUserReplicator,
        () -> ParameterUtils.getLdapSyncAttributes(
            authConfigRepository.findAllByTypeIn(AuthIntegrationType.LDAP.getName()).stream()
                .findFirst()
                .orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND))

        )
    );
  }

  @Bean
  public AuthenticationProvider activeDirectoryAuthProvider() {
    return new ActiveDirectoryAuthProvider(authConfigRepository, eventPublisher,
        activeDirectoryDetailsContextMapper(), new TokenServicesFacade(jwtEncoder()));
  }

  @Bean("activeDirectoryDetailsContextMapper")
  public DetailsContextMapper activeDirectoryDetailsContextMapper() {
    return new DetailsContextMapper(
        ldapUserReplicator,
        () -> ParameterUtils.getLdapSyncAttributes(
            authConfigRepository.findAllByTypeIn(AuthIntegrationType.ACTIVE_DIRECTORY.getName())
                .stream().findFirst()
                .orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND)))
    );
  }

  private String getSecret() {
    if (StringUtils.hasText(signingKey)) {
      return signingKey;
    }
    Optional<ServerSettings> secretKey = serverSettingsRepository.findByKey(SECRET_KEY);
    return secretKey.isPresent() ? secretKey.get().getValue()
        : serverSettingsRepository.generateSecret();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Order(5)
  public SecurityFilterChain globalWebSecurityFilterChain(
      HttpSecurity http,
      OAuth2AuthorizationRequestResolver authorizationRequestResolver,
      OAuthSuccessHandler successHandler) throws Exception {
    http
        .securityMatcher("/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/oauth/login/**",
                "/epam/**",
                "/info",
                "/health",
                "/api-docs",
                "/saml2/**",
                "/templates/**",
                "/login/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(new JwtReportPortalUserConverter(userDetailsService()))
            ))
        .csrf().disable()
        .formLogin().disable()
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(gitHubOAuth2UserService(gitHubUserReplicator, new OAuthRegistrationResource())))
            .clientRegistrationRepository(clientRegistrationRepository)
            .authorizationEndpoint(authorization -> authorization
                .baseUri("/oauth/login")
                .authorizationRequestResolver(authorizationRequestResolver)
            )
            .redirectionEndpoint(redirection -> redirection
                .baseUri("/sso/login/*")
            )
            .successHandler(successHandler)
            .failureHandler(authenticationFailureHandler)
        )
        .oauth2Client(Customizer.withDefaults());

    return http.build();
  }
  @Bean
  public GitHubOAuth2UserService gitHubOAuth2UserService(
      GitHubUserReplicator replicator,
      OAuthRegistrationResource registration) {

    return new GitHubOAuth2UserService(replicator, () -> registration);
  }

  @Bean
  @Order(10)
  public SecurityFilterChain ssoSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/sso/me/**", "/sso/internal/**", "/settings/**")
        .authorizeRequests(auth -> auth
            .requestMatchers("/settings/**").hasRole("ADMINISTRATOR")
            .requestMatchers("/sso/internal/**").hasRole("INTERNAL")
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    return http.build();
  }

  @Bean
  public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {

    return new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        "/oauth/login"
    );
  }

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {
    return new DefaultOAuth2AuthorizedClientManager(
        clientRegistrationRepository,
        authorizedClientRepository
    );
  }

  @Bean
  @Primary
  protected UserDetailsService userDetailsService() {
    return new DatabaseUserDetailsService();
  }

  @Bean
  public Map<String, OAuthProvider> oauthProviders(List<OAuthProvider> providers) {
    return providers.stream().collect(Collectors.toMap(OAuthProvider::getName, p -> p));
  }
}
