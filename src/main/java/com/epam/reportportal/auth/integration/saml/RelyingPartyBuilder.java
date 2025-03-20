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

package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.integration.github.GitHubClient;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.util.CertificationUtil;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Component
public class RelyingPartyBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(RelyingPartyBuilder.class);

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

  @Value("${rp.auth.saml.network-read-timeout}")
  private Integer networkReadTimeout;

  @Value("${rp.auth.saml.signed-requests}")
  private Boolean signedRequests;

  private static final String CALL_BACK_URL = "{baseUrl}/login/saml2/sso/{registrationId}";

  private static final String SAML_TYPE = "saml";

  private final IntegrationRepository integrationRepository;

  private final IntegrationTypeRepository integrationTypeRepository;

  @Value("${server.servlet.context-path}")
  private String pathValue;


  public RelyingPartyBuilder(IntegrationRepository integrationRepository,
      IntegrationTypeRepository integrationTypeRepository) {
    this.integrationRepository = integrationRepository;
    this.integrationTypeRepository = integrationTypeRepository;
  }


  public List<RelyingPartyRegistration> createRelyingPartyRegistrations() {
    IntegrationType samlIntegrationType = integrationTypeRepository.findByName(SAML_TYPE)
        .orElseThrow(() -> new RuntimeException("SAML Integration Type not found"));

    LOGGER.error("pathValue: " + pathValue);

    List<Integration> providers = integrationRepository.findAllGlobalByType(samlIntegrationType);

    List<RelyingPartyRegistration> registrations = providers.stream().map(provider -> {
      RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
          .fromMetadataLocation(SamlParameter.IDP_METADATA_URL.getParameter(provider).get())
          .registrationId(SamlParameter.IDP_NAME.getParameter(provider).get())
          .assertionConsumerServiceLocation(CALL_BACK_URL)
          .entityId(entityId)
          .signingX509Credentials((c) -> {
            if (signedRequests) {
              c.add(getSigningCredential());
            }
          })
          .assertingPartyDetails(party -> party.entityId(SamlParameter.IDP_NAME.getParameter(provider).get())
              .wantAuthnRequestsSigned(false)
              .singleSignOnServiceBinding(Saml2MessageBinding.POST))
          .build();
      return relyingPartyRegistration;

    }).toList();
    return registrations;
  }

  private Saml2X509Credential getSigningCredential() {
    X509Certificate certificate = CertificationUtil.getCertificateByName(keyAlias, keyStore, keyStorePassword);
    PrivateKey privateKey = CertificationUtil.getPrivateKey(keyAlias, keyPassword, keyStore, keyStorePassword);
    return new Saml2X509Credential(privateKey, certificate, Saml2X509Credential.Saml2X509CredentialType.SIGNING);
  }

}
