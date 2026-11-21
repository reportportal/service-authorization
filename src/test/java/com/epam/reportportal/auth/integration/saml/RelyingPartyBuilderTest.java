/*
 * Copyright 2026 EPAM Systems
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationParams;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.saml.SamlTestFixtures.Identity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.test.util.ReflectionTestUtils;

class RelyingPartyBuilderTest {

  private static final Identity IDP = Identity.generate("idp");

  @TempDir
  Path tempDir;

  private IntegrationRepository integrationRepository;
  private IntegrationType samlType;
  private RelyingPartyBuilder builder;

  @BeforeEach
  void setUp() {
    integrationRepository = mock(IntegrationRepository.class);
    IntegrationTypeRepository integrationTypeRepository = mock(IntegrationTypeRepository.class);
    samlType = new IntegrationType();
    samlType.setName("saml");
    when(integrationTypeRepository.findByName("saml")).thenReturn(Optional.of(samlType));

    builder = new RelyingPartyBuilder(integrationRepository, integrationTypeRepository);
    ReflectionTestUtils.setField(builder, "entityId", SamlTestFixtures.SP_ENTITY_ID);
    ReflectionTestUtils.setField(builder, "signedRequests", Boolean.FALSE);
  }

  private String metadata(String name, boolean withSigningCertificate) throws Exception {
    String keyDescriptor = withSigningCertificate
        ? "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo "
        + "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509Certificate>"
        + Base64.getEncoder().encodeToString(IDP.certificate().getEncoded())
        + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>"
        : "";
    String xml = "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\""
        + " entityID=\"" + SamlTestFixtures.IDP_ENTITY_ID + "\">"
        + "<md:IDPSSODescriptor"
        + " protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
        + keyDescriptor
        + "<md:SingleSignOnService"
        + " Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\""
        + " Location=\"https://idp.example.com/sso\"/>"
        + "</md:IDPSSODescriptor></md:EntityDescriptor>";
    return Files.writeString(tempDir.resolve(name + ".xml"), xml).toUri().toString();
  }

  private static Integration integration(String name, String metadataLocation, boolean enabled) {
    Map<String, Object> params = new HashMap<>();
    params.put(SamlParameter.IDP_NAME.getParameterName(), name);
    params.put(SamlParameter.IDP_METADATA_URL.getParameterName(), metadataLocation);
    Integration integration = new Integration();
    integration.setName(name);
    integration.setEnabled(enabled);
    integration.setParams(new IntegrationParams(params));
    return integration;
  }

  @Test
  void enabledIntegrationIsRegisteredWithVerificationCredentialsFromMetadata() throws Exception {
    when(integrationRepository.findAllGlobalByType(samlType)).thenReturn(
        List.of(integration("enabled-idp", metadata("enabled", true), true)));

    List<RelyingPartyRegistration> registrations = builder.createRelyingPartyRegistrations();

    assertEquals(1, registrations.size());
    RelyingPartyRegistration registration = registrations.get(0);
    assertEquals("enabled-idp", registration.getRegistrationId());
    assertEquals(SamlTestFixtures.SP_ENTITY_ID, registration.getEntityId());
    assertEquals("{baseUrl}/login/saml2/sso/{registrationId}",
        registration.getAssertionConsumerServiceLocation());
    var credentials = registration.getAssertingPartyMetadata().getVerificationX509Credentials();
    assertFalse(credentials.isEmpty());
    assertEquals(IDP.certificate(), credentials.iterator().next().getCertificate());
  }

  @Test
  void disabledIntegrationIsNotRegistered() throws Exception {
    when(integrationRepository.findAllGlobalByType(samlType)).thenReturn(List.of(
        integration("disabled-idp", metadata("disabled", true), false),
        integration("enabled-idp", metadata("enabled", true), true)));

    List<RelyingPartyRegistration> registrations = builder.createRelyingPartyRegistrations();

    assertEquals(List.of("enabled-idp"),
        registrations.stream().map(RelyingPartyRegistration::getRegistrationId).toList());
  }

  @Test
  void integrationWithoutVerificationCertificateIsNotRegistered() throws Exception {
    when(integrationRepository.findAllGlobalByType(samlType)).thenReturn(
        List.of(integration("no-cert-idp", metadata("no-cert", false), true)));

    assertTrue(builder.createRelyingPartyRegistrations().isEmpty());
  }
}
