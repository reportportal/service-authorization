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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.namespace.QName;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.w3c.dom.Element;

/**
 * Builds relying party registrations and (optionally signed) SAML responses for tests.
 */
public final class SamlTestFixtures {

  public static final String REGISTRATION_ID = "test-idp";
  public static final String SP_ENTITY_ID = "report.portal.sp.id";
  public static final String IDP_ENTITY_ID = "https://idp.example.com/metadata";
  public static final String ACS_LOCATION = "http://localhost/login/saml2/sso/" + REGISTRATION_ID;
  public static final String EMAIL_ATTRIBUTE = "mail";
  public static final String ROLES_ATTRIBUTE = "jobRole";

  static {
    OpenSamlInitializationService.initialize();
  }

  private SamlTestFixtures() {
  }

  /**
   * Key pair with a self-signed certificate.
   */
  public record Identity(KeyPair keyPair, X509Certificate certificate) {

    /**
     * Generates a new 2048-bit RSA key pair and a self-signed certificate for the given name.
     *
     * @param commonName common name (CN) put into the certificate subject
     * @return identity holding the key pair and certificate
     */
    public static Identity generate(String commonName) {
      try {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=" + commonName);
        Instant now = Instant.now();
        var holder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(now.toEpochMilli()),
            Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(1))),
            name, keyPair.getPublic())
            .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));
        return new Identity(keyPair, new JcaX509CertificateConverter().getCertificate(holder));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * Creates a relying party registration for the test service provider.
   *
   * @param acsLocation assertion consumer service URL of the test service provider
   * @param idpCertificates certificates the identity provider signs responses with
   * @return registration with the given ACS location and verification certificates
   */
  public static RelyingPartyRegistration registration(String acsLocation,
      X509Certificate... idpCertificates) {
    return RelyingPartyRegistration.withRegistrationId(REGISTRATION_ID)
        .entityId(SP_ENTITY_ID)
        .assertionConsumerServiceLocation(acsLocation)
        .assertingPartyMetadata(party -> party
            .entityId(IDP_ENTITY_ID)
            .singleSignOnServiceLocation("https://idp.example.com/sso")
            .verificationX509Credentials(credentials -> {
              for (X509Certificate certificate : idpCertificates) {
                credentials.add(Saml2X509Credential.verification(certificate));
              }
            }))
        .build();
  }

  /**
   * Creates a new builder for a SAML response.
   *
   * @return response builder with default values
   */
  public static ResponseBuilder response() {
    return new ResponseBuilder();
  }

  /**
   * Builder of a serialized SAML response with one assertion.
   */
  public static final class ResponseBuilder {

    private Identity signer;
    private boolean signAssertionOnly;
    private String nameId = "someone@example.com";
    private String destination = ACS_LOCATION;
    private String audience = SP_ENTITY_ID;
    private String inResponseTo;
    private Instant notOnOrAfter = Instant.now().plus(Duration.ofMinutes(10));
    private final Map<String, List<String>> attributes = new LinkedHashMap<>();

    /**
     * Sets the identity whose key signs the response or assertion.
     *
     * @param signer key pair and certificate used for signing
     * @return this builder
     */
    public ResponseBuilder signedBy(Identity signer) {
      this.signer = signer;
      return this;
    }

    /**
     * Requests that only the assertion is signed instead of the whole response.
     *
     * @return this builder
     */
    public ResponseBuilder signAssertionOnly() {
      this.signAssertionOnly = true;
      return this;
    }

    /**
     * Sets the NameID of the assertion subject.
     *
     * @param nameId subject identifier
     * @return this builder
     */
    public ResponseBuilder nameId(String nameId) {
      this.nameId = nameId;
      return this;
    }

    /**
     * Sets the Destination attribute of the response.
     *
     * @param destination URL the response is delivered to
     * @return this builder
     */
    public ResponseBuilder destination(String destination) {
      this.destination = destination;
      return this;
    }

    /**
     * Sets the audience restriction of the assertion.
     *
     * @param audience entity ID the assertion is addressed to
     * @return this builder
     */
    public ResponseBuilder audience(String audience) {
      this.audience = audience;
      return this;
    }

    /**
     * Sets the request ID the response claims to answer (the InResponseTo attribute).
     *
     * @param inResponseTo ID of the authentication request
     * @return this builder
     */
    public ResponseBuilder inResponseTo(String inResponseTo) {
      this.inResponseTo = inResponseTo;
      return this;
    }

    /**
     * Sets the instant after which the assertion is no longer valid.
     *
     * @param notOnOrAfter expiry instant of the assertion
     * @return this builder
     */
    public ResponseBuilder notOnOrAfter(Instant notOnOrAfter) {
      this.notOnOrAfter = notOnOrAfter;
      return this;
    }

    /**
     * Sets the values of an attribute, replacing any values set earlier under the same name.
     *
     * @param name attribute name
     * @param values attribute values
     * @return this builder
     */
    public ResponseBuilder attribute(String name, String... values) {
      this.attributes.put(name, List.of(values));
      return this;
    }

    /**
     * Adds the email attribute to the response.
     *
     * @param email value of the email attribute
     * @return this builder
     */
    public ResponseBuilder email(String email) {
      return attribute(EMAIL_ATTRIBUTE, email);
    }

    /**
     * Builds, optionally signs and serializes the response.
     *
     * @return response XML
     */
    public String build() {
      try {
        Instant now = Instant.now();

        Response response = create(Response.DEFAULT_ELEMENT_NAME);
        response.setID("_" + UUID.randomUUID());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setIssueInstant(now);
        response.setDestination(destination);
        response.setInResponseTo(inResponseTo);
        response.setIssuer(issuer());
        Status status = create(Status.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = create(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);
        response.setStatus(status);

        Assertion assertion = create(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID("_" + UUID.randomUUID());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(now);
        assertion.setIssuer(issuer());

        Subject subject = create(Subject.DEFAULT_ELEMENT_NAME);
        NameID nameIdElement = create(NameID.DEFAULT_ELEMENT_NAME);
        nameIdElement.setValue(nameId);
        subject.setNameID(nameIdElement);
        SubjectConfirmation confirmation = create(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        SubjectConfirmationData confirmationData =
            create(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        confirmationData.setRecipient(destination);
        confirmationData.setNotOnOrAfter(notOnOrAfter);
        confirmationData.setInResponseTo(inResponseTo);
        confirmation.setSubjectConfirmationData(confirmationData);
        subject.getSubjectConfirmations().add(confirmation);
        assertion.setSubject(subject);

        Conditions conditions = create(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(notOnOrAfter.minus(Duration.ofHours(1)));
        conditions.setNotOnOrAfter(notOnOrAfter);
        AudienceRestriction restriction = create(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        Audience audienceElement = create(Audience.DEFAULT_ELEMENT_NAME);
        audienceElement.setURI(audience);
        restriction.getAudiences().add(audienceElement);
        conditions.getAudienceRestrictions().add(restriction);
        assertion.setConditions(conditions);

        AuthnStatement authnStatement = create(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(now);
        AuthnContext authnContext = create(AuthnContext.DEFAULT_ELEMENT_NAME);
        AuthnContextClassRef classRef = create(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        classRef.setURI(AuthnContext.PASSWORD_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(classRef);
        authnStatement.setAuthnContext(authnContext);
        assertion.getAuthnStatements().add(authnStatement);

        if (!attributes.isEmpty()) {
          AttributeStatement statement = create(AttributeStatement.DEFAULT_ELEMENT_NAME);
          attributes.forEach((name, values) -> {
            Attribute attribute = create(Attribute.DEFAULT_ELEMENT_NAME);
            attribute.setName(name);
            for (String value : values) {
              XSString xmlValue = (XSString) XMLObjectProviderRegistrySupport.getBuilderFactory()
                  .<XSString>getBuilderOrThrow(XSString.TYPE_NAME)
                  .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
              xmlValue.setValue(value);
              attribute.getAttributeValues().add(xmlValue);
            }
            statement.getAttributes().add(attribute);
          });
          assertion.getAttributeStatements().add(statement);
        }

        response.getAssertions().add(assertion);

        SignableSAMLObject toSign = signAssertionOnly ? assertion : response;
        Signature signature = null;
        if (signer != null) {
          signature = create(Signature.DEFAULT_ELEMENT_NAME);
          signature.setSigningCredential(
              new BasicX509Credential(signer.certificate(), signer.keyPair().getPrivate()));
          signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
          signature.setCanonicalizationAlgorithm(
              SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
          toSign.setSignature(signature);
        }

        Element element = XMLObjectProviderRegistrySupport.getMarshallerFactory()
            .getMarshaller(response).marshall(response);
        if (signature != null) {
          Signer.signObject(signature);
        }
        return SerializeSupport.nodeToString(element);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    private static Issuer issuer() {
      Issuer issuer = create(Issuer.DEFAULT_ELEMENT_NAME);
      issuer.setValue(IDP_ENTITY_ID);
      return issuer;
    }

    @SuppressWarnings("unchecked")
    private static <T extends XMLObject> T create(QName name) {
      return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(name)
          .buildObject(name);
    }
  }
}
