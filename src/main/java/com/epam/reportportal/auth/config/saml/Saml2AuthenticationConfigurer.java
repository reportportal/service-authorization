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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class Saml2AuthenticationConfigurer extends
    SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Saml2AuthenticationConfigurer.class);

  private SamlAuthSuccessHandler successHandler;

  private AuthFailureHandler failureHandler;

  private SamlUserReplicator samlUserReplicator;

  private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

  public Saml2AuthenticationConfigurer(SamlAuthSuccessHandler successHandler,
      AuthFailureHandler failureHandler, SamlUserReplicator samlUserReplicator,
      RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
    this.successHandler = successHandler;
    this.failureHandler = failureHandler;
    this.samlUserReplicator = samlUserReplicator;
    this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
  }

  @Override
  public void configure(HttpSecurity http) {
    LOGGER.error("Saml2AuthenticationConfigurer: " + http);
    Saml2WebSsoAuthenticationFilter saml2Filter = new Saml2WebSsoAuthenticationFilter(relyingPartyRegistrationRepository);
    saml2Filter.setAuthenticationManager(new ReportPortalSamlAuthenticationManager(samlUserReplicator));
    saml2Filter.setAuthenticationSuccessHandler(successHandler);
    saml2Filter.setAuthenticationFailureHandler(failureHandler);

    http.addFilterAfter(saml2Filter, Saml2WebSsoAuthenticationFilter.class);
  }
}
