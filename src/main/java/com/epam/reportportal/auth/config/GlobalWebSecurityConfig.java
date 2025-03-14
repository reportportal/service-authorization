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
import com.epam.reportportal.auth.basic.BasicPasswordAuthenticationProvider;
import com.epam.reportportal.auth.basic.DatabaseUserDetailsService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
public class GlobalWebSecurityConfig {

  public static final String SSO_LOGIN_PATH = "/sso/login";

  private static final String SECRET_KEY = "secret.key";

  @Value("${rp.jwt.signing-key}")
  private String signingKey;

  private final AuthenticationFailureHandler authenticationFailureHandler;
  private final IntegrationRepository authConfigRepository;
  private final LdapUserReplicator ldapUserReplicator;
  private final List<OAuthProvider> authProviders;
  private final ApplicationEventPublisher eventPublisher;

  private final MutableClientRegistrationRepository clientRegistrationRepository;

  private final GitHubUserReplicator gitHubUserReplicator;

  @Autowired
  private ServerSettingsRepository serverSettingsRepository;


//  @Autowired
//  @Qualifier("activeDirectoryDetailsContextMapper")
//  DetailsContextMapper activeDirectoryContextMapper;
//
//  @Autowired
//  @Qualifier("ldapDetailsContextMapper")
//  DetailsContextMapper ldapContextMapper;

  @Autowired
  public GlobalWebSecurityConfig(
      AuthenticationFailureHandler authenticationFailureHandler,
      IntegrationRepository authConfigRepository,
      LdapUserReplicator ldapUserReplicator,
      List<OAuthProvider> authProviders,
      ApplicationEventPublisher eventPublisher,
      MutableClientRegistrationRepository clientRegistrationRepository,
      GitHubUserReplicator gitHubUserReplicator) {

    this.authenticationFailureHandler = authenticationFailureHandler;
    this.authConfigRepository = authConfigRepository;
    this.ldapUserReplicator = ldapUserReplicator;
    this.authProviders = authProviders;
    this.eventPublisher = eventPublisher;
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.gitHubUserReplicator = gitHubUserReplicator;
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

  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(getSecret().getBytes(), "HmacSHA256")).build();
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

//  @Bean
//  public AuthenticationProvider basicPasswordAuthProvider() {
//    BasicPasswordAuthenticationProvider provider = new BasicPasswordAuthenticationProvider();
//    provider.setUserDetailsService(userDetailsService());
//    provider.setPasswordEncoder(passwordEncoder());
//    return provider;
//  }

  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

//  @Bean
//  public AuthenticationProvider activeDirectoryAuthProvider() {
//    return new ActiveDirectoryAuthProvider(authConfigRepository, eventPublisher,
//        activeDirectoryContextMapper);
//  }
//
//  @Bean
//  public AuthenticationProvider ldapAuthProvider() {
//    return new LdapAuthProvider(authConfigRepository, eventPublisher, ldapContextMapper);
//  }
//
//  @Bean("activeDirectoryDetailsContextMapper")
//  public DetailsContextMapper activeDirectoryDetailsContextMapper() {
//    return new DetailsContextMapper(
//        ldapUserReplicator,
//        () -> ParameterUtils.getLdapSyncAttributes(
//            authConfigRepository.findAllByTypeIn(AuthIntegrationType.ACTIVE_DIRECTORY.getName())
//                .stream().findFirst()
//                .orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND)))
//    );
//  }
//
//  @Bean("ldapDetailsContextMapper")
//  public DetailsContextMapper ldapDetailsContextMapper() {
//    return new DetailsContextMapper(
//        ldapUserReplicator,
//        () -> ParameterUtils.getLdapSyncAttributes(
//            authConfigRepository.findAllByTypeIn(AuthIntegrationType.LDAP.getName()).stream()
//                .findFirst()
//                .orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND))
//
//        )
//    );
//  }

//  @Bean
//  public AuthenticationEventPublisher authenticationEventPublisher(
//      ApplicationEventPublisher applicationEventPublisher) {
//    return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
//  }

//  @Bean
//  @Primary
//  public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//    return http.getSharedObject(AuthenticationManagerBuilder.class)
//        .authenticationProvider(basicPasswordAuthProvider())
//        .authenticationEventPublisher(authenticationEventPublisher(eventPublisher))
////        .authenticationProvider(ldapAuthProvider())
////        .authenticationProvider(activeDirectoryAuthProvider())
//        .build();
//  }

  @Bean
  public Map<String, OAuthProvider> oauthProviders(List<OAuthProvider> providers) {
    return providers.stream().collect(Collectors.toMap(OAuthProvider::getName, p -> p));
  }

}
