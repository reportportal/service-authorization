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
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
public class RelyingPartyConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(RelyingPartyConfiguration.class);

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

  @Bean
  public RelyingPartyRegistrationRepository relyingParty() {
    IntegrationType samlIntegrationType = integrationTypeRepository.findByName(AuthIntegrationType.SAML.getName())
        .orElseThrow(() -> new RuntimeException("SAML Integration Type not found"));

    List<Integration> providers = integrationRepository.findAllGlobalByType(samlIntegrationType);

//    X509Certificate certificate = CertificationUtil.getCertificateByName(keyAlias, keyStore,
//        keyStorePassword);
//    Saml2X509Credential credential = Saml2X509Credential.verification(certificate);

    List<RelyingPartyRegistration> registrations = providers.stream().map(provider -> {
      RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
          .fromMetadataLocation(SamlParameter.IDP_METADATA_URL.getParameter(provider).get())
          .registrationId(SamlParameter.IDP_NAME.getParameter(provider).get())
          .entityId(entityId)
          .assertionConsumerServiceLocation(samlIntegrationType.getDetails().getDetails().get("callbackUrl").toString())
          .assertingPartyDetails(party -> party.entityId(SamlParameter.IDP_NAME.getParameter(provider).get())
              .wantAuthnRequestsSigned(false)
//              .singleSignOnServiceLocation(samlProperties.getAssertingpParty().getServiceLocation())
              .singleSignOnServiceBinding(Saml2MessageBinding.POST))
//          .signingX509Credentials(c -> c.add(credential))
          .build();
      return relyingPartyRegistration;

    }).collect(Collectors.toList());
    String listAsString = registrations.stream()
        .map(RelyingPartyRegistration::getRegistrationId)
        .collect(Collectors.joining(", "));
    LOGGER.error("RelyingPartyRegistration: " + listAsString);
    return new InMemoryRelyingPartyRegistrationRepository(registrations);
  }
}
