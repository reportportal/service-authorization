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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.epam.reportportal.auth.model.saml.SamlResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

/**
 * Regression test for EPMRPP-118917: SamlResponseParser must not resolve DOCTYPE-declared external entities (XXE).
 * Before commit 8313105c, DocumentBuilderFactory had no hardening flags, so the crafted payload below would read a
 * local file and smuggle its content into the parsed {@code Issuer} value. After the fix, DOCTYPE declarations are
 * rejected outright.
 */
class SamlResponseParserXxeTest {

  private static final String SECRET_MARKER = "TOP-SECRET-XXE-MARKER-42";

  private Path secretFile;

  @Test
  void shouldNotResolveExternalEntityFromDoctype() throws Exception {
    secretFile = Files.createTempFile("xxe-secret", ".txt");
    Files.writeString(secretFile, SECRET_MARKER);

    String maliciousSaml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE saml2p:Response [
          <!ENTITY xxe SYSTEM "file://%s">
        ]>
        <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                          xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
                          ID="_response1" Version="2.0" IssueInstant="2026-09-01T00:00:00Z">
          <saml2:Issuer>&xxe;</saml2:Issuer>
          <saml2p:Status>
            <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
          </saml2p:Status>
          <saml2:Assertion ID="_assertion1" IssueInstant="2026-09-01T00:00:00Z" Version="2.0">
            <saml2:Issuer>test-idp</saml2:Issuer>
            <saml2:Subject>
              <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">user@example.com</saml2:NameID>
            </saml2:Subject>
          </saml2:Assertion>
        </saml2p:Response>
        """.formatted(secretFile.toAbsolutePath());

    try {
      SamlResponse response = SamlResponseParser.parseSamlResponse(maliciousSaml);
      // Reaching here means the parser expanded the entity instead of rejecting the DOCTYPE.
      boolean leaked = response.getIssuer() != null && response.getIssuer().contains(SECRET_MARKER);
      if (leaked) {
        fail("XXE VULNERABLE: local file content was disclosed via external entity expansion. "
            + "Issuer value = " + response.getIssuer());
      } else {
        fail("Parser accepted a DOCTYPE-bearing payload instead of rejecting it "
            + "(disallow-doctype-decl not enforced), even though this particular entity "
            + "did not resolve.");
      }
    } catch (Exception e) {
      Throwable cause = e;
      while (cause != null && !(cause instanceof SAXParseException)) {
        cause = cause.getCause();
      }
      assertNotNull(cause, "Expected a SAXParseException caused by the disallow-doctype-decl feature, "
          + "but got: " + e);
      assertTrue(cause.getMessage() != null && cause.getMessage().toLowerCase().contains("doctype"),
          "Expected the failure to be about the disallowed DOCTYPE declaration, "
              + "but got: " + cause.getMessage());
    }
  }

  @AfterEach
  void cleanup() throws Exception {
    if (secretFile != null) {
      Files.deleteIfExists(secretFile);
    }
  }
}
