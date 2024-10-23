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

import static org.springframework.security.config.Customizer.withDefaults;

import com.epam.reportportal.auth.AuthFailureHandler;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.saml.ReportPortalSamlAuthenticationManager;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import com.epam.reportportal.auth.util.CertificationUtil;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
@Order(4)
public class SamlSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Value("${rp.auth.saml.base-path}")
  private String basePath;

  @Value("${rp.auth.saml.entity-id}")
  private String entityId;

  @Value("${rp.auth.saml.key-alias}")
  private String keyAlias;

  @Value("${rp.auth.saml.key-password}")
  private String keyPassword;

  @Value("${rp.auth.saml.key-store}")
  private String keyStore;

  @Value("${rp.auth.saml.key-store-password}")
  private String keyStorePassword;

  @Value("${rp.auth.saml.active-key-name}")
  private String activeKeyName;

  @Value("${rp.auth.saml.network-connection-timeout}")
  private Integer networkConnectTimeout;

  @Value("${rp.auth.saml.network-read-timeout}")
  private Integer networkReadTimeout;

  @Value("${rp.auth.saml.signed-requests}")
  private Boolean signedRequests;

  @Value("${rp.auth.saml.prefix}")
  private String prefix;

  @Autowired
  private IntegrationTypeRepository integrationTypeRepository;

  @Autowired
  private IntegrationRepository integrationRepository;

  @Autowired
  private SamlAuthSuccessHandler successHandler;

  @Autowired
  private AuthFailureHandler failureHandler;

  @Autowired
  private SamlUserReplicator samlUserReplicator;


  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // add auto-generation of ServiceProvider Metadata
    Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingParty());
    Saml2MetadataFilter filter = new Saml2MetadataFilter(relyingPartyRegistrationResolver, new OpenSamlMetadataResolver());

    http
        // Configure SAML 2.0 Login
        .saml2Login(
            samlLogin ->
                samlLogin.loginPage("/saml/sp/discovery")
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
                    .authenticationManager(new ReportPortalSamlAuthenticationManager(samlUserReplicator))

        )
        .addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class)
        .build();

  }

  @Bean
  public RelyingPartyRegistrationRepository relyingParty() throws Exception {
    IntegrationType samlIntegrationType = integrationTypeRepository.findByName("SAML")
        .orElseThrow(() -> new RuntimeException("SAML Integration Type not found"));

    List<Integration> providers = integrationRepository.findAllGlobalByType(samlIntegrationType);

    X509Certificate certificate = CertificationUtil.getCertificateByName(keyAlias, keyStore,
        keyStorePassword);
    Saml2X509Credential credential = Saml2X509Credential.verification(certificate);

    List<RelyingPartyRegistration> registrations = providers.stream().map(provider -> {
      RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
          .fromMetadataLocation(SamlParameter.IDP_METADATA_URL.getParameter(provider).get())
          .registrationId("report.portal.sp.id")
          .entityId(entityId)
          .assertionConsumerServiceLocation(SamlParameter.BASE_PATH.getParameter(provider).get())
          .assertingPartyDetails(party -> party.entityId(SamlParameter.IDP_NAME.getParameter(provider).get())
              .wantAuthnRequestsSigned(false)
//              .singleSignOnServiceLocation(samlProperties.getAssertingpParty().getServiceLocation())
              .singleSignOnServiceBinding(Saml2MessageBinding.POST))
          .signingX509Credentials(c -> c.add(credential))
          .build();
      return relyingPartyRegistration;

    }).collect(Collectors.toList());
    return new InMemoryRelyingPartyRegistrationRepository(registrations);
  }
}
