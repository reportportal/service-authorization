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
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.util.CertificationUtil;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Slf4j
@Component
public class RelyingPartyBuilder {

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

  @Value("${rp.auth.saml.network-read-timeout:10000}")
  private int networkReadTimeout;

  @Value("${rp.auth.saml.signed-requests}")
  private Boolean signedRequests;

  @Value("${rp.auth.saml.network-connection-timeout:5000}")
  private int networkConnectionTimeout;

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
    try {
      System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(networkReadTimeout));
      System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(networkConnectionTimeout));
    } catch (SecurityException se) {
      log.warn("Unable to set default network timeouts: {}", se.getMessage());
    }
    var samlIntegrationType = integrationTypeRepository.findByName(SAML_TYPE)
        .orElseThrow(() -> new RuntimeException("SAML Integration Type not found"));

    var providers = integrationRepository.findAllGlobalByType(samlIntegrationType);

    var registrations = providers.stream()
        .flatMap(provider -> {
          try {
            var metadataLocation = SamlParameter.IDP_METADATA_URL.getParameter(provider)
                .orElseThrow(() -> new IllegalStateException("IDP metadata URL is missing"));
            var registrationId = SamlParameter.IDP_NAME.getParameter(provider)
                .orElseThrow(() -> new IllegalStateException("IDP name is missing"));

            var rp = RelyingPartyRegistrations
                .fromMetadataLocation(metadataLocation)
                .registrationId(registrationId)
                .assertionConsumerServiceLocation(getCallBackUrl())
                .entityId(entityId)
                .signingX509Credentials((c) -> {
                  if (Boolean.TRUE.equals(signedRequests)) {
                    c.add(getSigningCredential());
                  }
                })
                .assertingPartyMetadata(meta -> meta
                    .entityId(registrationId)
                    .wantAuthnRequestsSigned(false)
                    .singleSignOnServiceBinding(Saml2MessageBinding.POST)
                )
                .build();
            return Stream.of(rp);
          } catch (Exception e) {
            log.warn("Skipping SAML provider due to metadata error: {}", e.getMessage());
            return Stream.empty();
          }
        })
        .toList();

    if (registrations.isEmpty()) {
      log.warn("No valid SAML providers registered. SAML login will be unavailable.");
    }

    return registrations;
  }

  private Saml2X509Credential getSigningCredential() {
    X509Certificate certificate = CertificationUtil.getCertificateByName(keyAlias, keyStore, keyStorePassword);
    PrivateKey privateKey = CertificationUtil.getPrivateKey(keyAlias, keyPassword, keyStore, keyStorePassword);
    return new Saml2X509Credential(privateKey, certificate, Saml2X509Credential.Saml2X509CredentialType.SIGNING);
  }

  private String getCallBackUrl() {
    return StringUtils.isEmpty(pathValue) || pathValue.equals("/") ? CALL_BACK_URL.replaceFirst("baseUrl}/","baseUrl}/uat/" ) : CALL_BACK_URL;
  }
}
