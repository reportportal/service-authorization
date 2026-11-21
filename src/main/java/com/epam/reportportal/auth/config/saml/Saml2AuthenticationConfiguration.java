/*
 * Copyright 2024 EPAM Systems
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

package com.epam.reportportal.auth.config.saml;

import com.epam.reportportal.auth.AuthFailureHandler;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlResponseAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SAML 2.0 login configuration.
 *
 * <p>The assertion consumer endpoint is served by the single
 * {@code Saml2WebSsoAuthenticationFilter} that {@code saml2Login()} configures. Its authentication
 * manager contains only Spring Security's {@link OpenSaml4AuthenticationProvider}, so every SAML
 * response is validated (signature against the identity provider's registered verification
 * credentials, issuer, destination, InResponseTo, conditions, audience, subject confirmation)
 * before {@link SamlResponseAuthenticationConverter} maps the validated assertion to a
 * ReportPortal user. Do not register another filter or authentication manager for the assertion
 * consumer URL: nothing may turn a {@code Saml2AuthenticationToken} into an authenticated
 * principal without passing through this provider.
 *
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
public class Saml2AuthenticationConfiguration {

  private static final String SAML_PROCESSING_URL = "/login/saml2/sso/{registrationId}";

  private final SamlAuthSuccessHandler successHandler;

  private final AuthFailureHandler failureHandler;

  private final SamlResponseAuthenticationConverter responseAuthenticationConverter;

  private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

  public Saml2AuthenticationConfiguration(SamlAuthSuccessHandler successHandler,
      AuthFailureHandler failureHandler,
      SamlResponseAuthenticationConverter responseAuthenticationConverter,
      RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
    this.successHandler = successHandler;
    this.failureHandler = failureHandler;
    this.responseAuthenticationConverter = responseAuthenticationConverter;
    this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
  }

  /**
   * Creates the authentication manager for SAML responses. It consists solely of the validating
   * OpenSAML provider; the ReportPortal specific part is plugged in as the provider's response
   * authentication converter, which is invoked only after the response and its assertions have
   * been validated.
   *
   * <p>Deliberately not exposed as a bean, so it does not affect the other authentication
   * managers of the application.
   *
   * @param responseAuthenticationConverter converter of the validated response
   * @return {@link AuthenticationManager} for the assertion consumer endpoint
   */
  public static AuthenticationManager samlAuthenticationManager(
      SamlResponseAuthenticationConverter responseAuthenticationConverter) {
    OpenSaml4AuthenticationProvider provider = new OpenSaml4AuthenticationProvider();
    provider.setResponseAuthenticationConverter(responseAuthenticationConverter);
    return new ProviderManager(provider);
  }

  @Bean
  @Order(4)
  public SecurityFilterChain samlSecurityFilterChain(HttpSecurity http) throws Exception {
    Saml2RegistrationValidationFilter validationFilter =
        new Saml2RegistrationValidationFilter(relyingPartyRegistrationRepository, failureHandler);

    http
        .securityMatcher("/saml2/**", "/login/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/saml2/**").permitAll()
            .anyRequest().authenticated()
        )
        .saml2Login(saml2 -> saml2
            .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
            .loginProcessingUrl(SAML_PROCESSING_URL)
            .authenticationManager(samlAuthenticationManager(responseAuthenticationConverter))
            .successHandler(successHandler)
            .failureHandler(failureHandler)
        )
        .addFilterBefore(validationFilter, Saml2WebSsoAuthenticationRequestFilter.class)
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
