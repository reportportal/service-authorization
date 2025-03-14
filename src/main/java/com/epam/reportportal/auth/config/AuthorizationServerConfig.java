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

import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import com.epam.reportportal.auth.basic.BasicPasswordAuthenticationProvider;
import com.epam.reportportal.auth.config.password.CustomCodeGrantAuthenticationConverter;
import com.epam.reportportal.auth.config.password.CustomCodeGrantAuthenticationProvider;
import com.epam.reportportal.auth.config.password.OAuth2ErrorResponseHandler;
import com.epam.reportportal.auth.config.password.PasswordGrantTokenGenerator;
import com.epam.reportportal.auth.config.utils.JwtReportPortalUserConverter;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.ServerSettingsRepository;
import com.epam.reportportal.auth.entity.ServerSettings;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.ldap.ActiveDirectoryAuthProvider;
import com.epam.reportportal.auth.integration.ldap.DetailsContextMapper;
import com.epam.reportportal.auth.integration.ldap.LdapAuthProvider;
import com.epam.reportportal.auth.integration.ldap.LdapUserReplicator;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

  private static final String SECRET_KEY = "secret.key";

  @Value("${rp.jwt.signing-key}")
  private String signingKey;

  @Value("${rp.jwt.token.validity-period}")
  private Integer tokenValidity;

//  @Autowired
//  private AuthenticationManager authenticationManager;

  @Autowired
  private ServerSettingsRepository serverSettingsRepository;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private IntegrationRepository authConfigRepository;

  @Autowired
  private LdapUserReplicator ldapUserReplicator;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

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
}
