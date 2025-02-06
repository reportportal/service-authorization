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
import com.epam.reportportal.auth.integration.saml.ReportPortalSamlAuthenticationManager;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
@Order(4)
public class Saml2AuthenticationConfiguration extends
    WebSecurityConfigurerAdapter {

  private final SamlAuthSuccessHandler successHandler;

  private final AuthFailureHandler failureHandler;

  private final SamlUserReplicator samlUserReplicator;

  private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

  public Saml2AuthenticationConfiguration(SamlAuthSuccessHandler successHandler,
      AuthFailureHandler failureHandler, SamlUserReplicator samlUserReplicator,
      RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
    this.successHandler = successHandler;
    this.failureHandler = failureHandler;
    this.samlUserReplicator = samlUserReplicator;
    this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {

    String samlProcessingUrl = "/login/saml2/sso/{registrationId}";

    Saml2WebSsoAuthenticationFilter saml2Filter = new Saml2WebSsoAuthenticationFilter(
        relyingPartyRegistrationRepository,
        samlProcessingUrl
    );
    saml2Filter.setAuthenticationManager(new ReportPortalSamlAuthenticationManager(samlUserReplicator));
    saml2Filter.setAuthenticationSuccessHandler(successHandler);
    saml2Filter.setAuthenticationFailureHandler(failureHandler);

    http
        .securityMatchers()
        .requestMatchers("/saml2/**","/login/**")
        .and()
        .authorizeHttpRequests(auth -> auth.requestMatchers( "/saml2/**").permitAll().anyRequest().authenticated())
        .saml2Login(Customizer.withDefaults())
        .addFilterBefore(saml2Filter, Saml2WebSsoAuthenticationFilter.class)
        .csrf().disable();
  }
}