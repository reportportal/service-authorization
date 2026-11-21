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

import static com.epam.reportportal.auth.integration.saml.SamlTestFixtures.ACS_LOCATION;
import static com.epam.reportportal.auth.integration.saml.SamlTestFixtures.EMAIL_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.saml.SamlTestFixtures.REGISTRATION_ID;
import static com.epam.reportportal.auth.integration.saml.SamlTestFixtures.ROLES_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.epam.reportportal.auth.binary.UserBinaryDataService;
import com.epam.reportportal.auth.commons.ContentTypeResolver;
import com.epam.reportportal.auth.config.saml.Saml2AuthenticationConfiguration;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.IntegrationTypeRepository;
import com.epam.reportportal.auth.dao.ProjectRepository;
import com.epam.reportportal.auth.dao.UserRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationParams;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.entity.user.UserRole;
import com.epam.reportportal.auth.entity.user.UserType;
import com.epam.reportportal.auth.event.UserEventPublisher;
import com.epam.reportportal.auth.integration.converter.SamlConverter;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.saml.SamlTestFixtures.Identity;
import com.epam.reportportal.auth.integration.validator.request.SamlUpdateAuthRequestValidator;
import com.epam.reportportal.auth.integration.validator.request.param.provider.SamlRequiredParamNamesProvider;
import com.epam.reportportal.auth.model.integration.auth.UpdateAuthRQ;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import com.epam.reportportal.auth.util.PersonalProjectService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/**
 * Verifies that the SAML authentication manager used by the assertion consumer endpoint validates
 * responses before any user is looked up or created, and that account linking and role mapping
 * are strict. No Spring context or database is needed: the real authentication manager, converter
 * and replicator are used, only the repositories are mocked.
 */
class SamlResponseValidationTest {

  private static final String ADMIN_EMAIL = "admin@example.com";

  private static Identity idp;
  private static Identity attacker;

  private UserRepository userRepository;
  private IntegrationRepository integrationRepository;
  private UserEventPublisher userEventPublisher;
  private Integration integration;
  private AuthenticationManager authenticationManager;
  private RelyingPartyRegistration registration;

  @BeforeAll
  static void generateKeys() {
    idp = Identity.generate("idp");
    attacker = Identity.generate("attacker");
  }

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    integrationRepository = mock(IntegrationRepository.class);
    userEventPublisher = mock(UserEventPublisher.class);
    IntegrationTypeRepository integrationTypeRepository = mock(IntegrationTypeRepository.class);

    IntegrationType samlType = new IntegrationType();
    samlType.setName("saml");
    when(integrationTypeRepository.findByName("saml")).thenReturn(Optional.of(samlType));

    Map<String, Object> params = new HashMap<>();
    params.put(SamlParameter.IDP_NAME.getParameterName(), REGISTRATION_ID);
    params.put(SamlParameter.EMAIL_ATTRIBUTE.getParameterName(), EMAIL_ATTRIBUTE);
    params.put(SamlParameter.ROLES_ATTRIBUTE.getParameterName(), ROLES_ATTRIBUTE);
    integration = new Integration();
    integration.setName(REGISTRATION_ID);
    integration.setEnabled(true);
    integration.setParams(new IntegrationParams(params));
    when(integrationRepository.findAllGlobalByType(samlType)).thenReturn(List.of(integration));

    when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

    SamlUserReplicator replicator = new SamlUserReplicator(userRepository,
        mock(ProjectRepository.class), mock(PersonalProjectService.class),
        mock(UserBinaryDataService.class), integrationTypeRepository, integrationRepository,
        mock(ContentTypeResolver.class), userEventPublisher);

    authenticationManager = Saml2AuthenticationConfiguration.samlAuthenticationManager(
        new SamlResponseAuthenticationConverter(replicator));
    registration = SamlTestFixtures.registration(ACS_LOCATION, idp.certificate());
  }

  private Authentication authenticate(String responseXml) {
    return authenticationManager.authenticate(
        new Saml2AuthenticationToken(registration, responseXml));
  }

  private void assertRejectedBeforeUserLookup(String expectedErrorCode, String responseXml) {
    Saml2AuthenticationException exception =
        assertThrows(Saml2AuthenticationException.class, () -> authenticate(responseXml));
    assertEquals(expectedErrorCode, exception.getSaml2Error().getErrorCode(),
        exception.getMessage());
    verifyNoInteractions(userRepository, integrationRepository, userEventPublisher);
  }

  private static User user(String email, UserType type, UserRole role, boolean active) {
    User user = new User();
    user.setLogin(email);
    user.setEmail(email);
    user.setUserType(type);
    user.setRole(role);
    user.setActive(active);
    return user;
  }

  // ---- response validation -------------------------------------------------------------------

  @Test
  void unsignedResponseIsRejected() {
    when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(
        Optional.of(user(ADMIN_EMAIL, UserType.INTERNAL, UserRole.ADMINISTRATOR, true)));

    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_SIGNATURE, SamlTestFixtures.response()
        .nameId(ADMIN_EMAIL).email(ADMIN_EMAIL).build());
  }

  @Test
  void responseSignedWithWrongKeyIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_SIGNATURE, SamlTestFixtures.response()
        .signedBy(attacker).nameId(ADMIN_EMAIL).email(ADMIN_EMAIL).build());
  }

  @Test
  void assertionSignedWithWrongKeyIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_SIGNATURE, SamlTestFixtures.response()
        .signedBy(attacker).signAssertionOnly().email(ADMIN_EMAIL).build());
  }

  @Test
  void expiredAssertionIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_ASSERTION, SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com")
        .notOnOrAfter(Instant.now().minus(Duration.ofMinutes(30))).build());
  }

  @Test
  void responseTamperedAfterSigningIsRejected() {
    String signed = SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com").build();
    assertTrue(signed.contains("user@example.com"));

    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_SIGNATURE,
        signed.replace("user@example.com", ADMIN_EMAIL));
  }

  @Test
  void responseForAnotherAudienceIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_ASSERTION, SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com").audience("some.other.sp").build());
  }

  @Test
  void responseForAnotherDestinationIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_DESTINATION, SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com")
        .destination("https://other.example.com/login/saml2/sso/" + REGISTRATION_ID).build());
  }

  @Test
  void responseWithUnsolicitedInResponseToIsRejected() {
    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_IN_RESPONSE_TO,
        SamlTestFixtures.response().signedBy(idp).email("user@example.com")
            .inResponseTo("_not-issued-by-us").build());
  }

  @Test
  void responseWithInResponseToNotMatchingStoredRequestIsRejected() {
    AbstractSaml2AuthenticationRequest storedRequest =
        mock(AbstractSaml2AuthenticationRequest.class);
    when(storedRequest.getId()).thenReturn("_issued-request");
    String response = SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com").inResponseTo("_another-request").build();

    Saml2AuthenticationException exception = assertThrows(Saml2AuthenticationException.class,
        () -> authenticationManager.authenticate(
            new Saml2AuthenticationToken(registration, response, storedRequest)));
    assertEquals(Saml2ErrorCodes.INVALID_IN_RESPONSE_TO,
        exception.getSaml2Error().getErrorCode(), exception.getMessage());
    verifyNoInteractions(userRepository, integrationRepository);
  }

  @Test
  void responseIsRejectedWhenRegistrationHasNoVerificationCredentials() {
    registration = SamlTestFixtures.registration(ACS_LOCATION);

    assertRejectedBeforeUserLookup(Saml2ErrorCodes.INVALID_SIGNATURE, SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com").build());
  }

  @Test
  void doctypeWithExternalEntityIsRejected(@TempDir Path tempDir) throws Exception {
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "xxe-marker@example.com");
    String valid = SamlTestFixtures.response().email("placeholder@example.com").build();
    String body = valid.substring(valid.indexOf("<saml2p:Response"));
    assertTrue(body.contains("placeholder@example.com"));
    String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>\n"
        + body.replace("placeholder@example.com", "&xxe;");

    Saml2AuthenticationException exception =
        assertThrows(Saml2AuthenticationException.class, () -> authenticate(payload));
    assertEquals(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA,
        exception.getSaml2Error().getErrorCode(), exception.getMessage());
    assertFalse(String.valueOf(exception.getMessage()).contains("xxe-marker"));
    verifyNoInteractions(userRepository, integrationRepository, userEventPublisher);
  }

  @Test
  void doctypeWithInternalEntityIsRejected() {
    String valid = SamlTestFixtures.response().email("placeholder@example.com").build();
    String body = valid.substring(valid.indexOf("<saml2p:Response"));
    String payload = "<!DOCTYPE foo [<!ENTITY a \"" + ADMIN_EMAIL + "\">]>"
        + body.replace("placeholder@example.com", "&a;");

    assertRejectedBeforeUserLookup(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA, payload);
  }

  // ---- successful authentication -------------------------------------------------------------

  @Test
  void correctlySignedResponseAuthenticatesExistingSamlUser() {
    when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(
        Optional.of(user("user@example.com", UserType.SAML, UserRole.USER, true)));

    Authentication result = authenticate(SamlTestFixtures.response()
        .signedBy(idp).nameId("opaque-name-id").email("User@Example.com").build());

    assertInstanceOf(Saml2Authentication.class, result);
    assertTrue(result.isAuthenticated());
    assertEquals("user@example.com", result.getName());
    assertEquals(List.of(UserRole.USER.getAuthority()),
        result.getAuthorities().stream().map(Object::toString).toList());
    verify(userRepository, never()).save(any());
  }

  @Test
  void correctlySignedAssertionInUnsignedResponseAuthenticates() {
    Authentication result = authenticate(SamlTestFixtures.response()
        .signedBy(idp).signAssertionOnly().email("new.user@example.com").build());

    assertTrue(result.isAuthenticated());
    assertEquals("new.user@example.com", result.getName());
  }

  @Test
  void newUserIsCreatedWithTheSameIdentifierThatIsUsedForLookup() {
    Authentication result = authenticate(SamlTestFixtures.response()
        .signedBy(idp).nameId("opaque-name-id").email("New.User@example.com").build());

    verify(userRepository).findByEmailIgnoreCase("new.user@example.com");
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertEquals("new.user@example.com", saved.getValue().getEmail());
    assertEquals("new.user@example.com", saved.getValue().getLogin());
    assertEquals(UserType.SAML, saved.getValue().getUserType());
    assertEquals(UserRole.USER, saved.getValue().getRole());
    assertEquals("new.user@example.com", result.getName());
  }

  // ---- account linking -----------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(strings = {"INTERNAL", "LDAP", "GITHUB", "UPSA", "SCIM"})
  void existingAccountOfAnotherTypeIsRefused(String type) {
    when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(
        user(ADMIN_EMAIL, UserType.valueOf(type), UserRole.ADMINISTRATOR, true)));

    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> authenticate(SamlTestFixtures.response()
            .signedBy(idp).nameId(ADMIN_EMAIL).email(ADMIN_EMAIL).build()));

    assertTrue(exception.getMessage().contains("already exists"), exception.getMessage());
    verify(userRepository, never()).save(any());
  }

  @Test
  void inactiveSamlUserIsRefused() {
    when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(
        Optional.of(user("user@example.com", UserType.SAML, UserRole.USER, false)));

    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> authenticate(SamlTestFixtures.response()
            .signedBy(idp).email("user@example.com").build()));

    assertTrue(exception.getMessage().contains("locked"), exception.getMessage());
    verify(userRepository, never()).save(any());
  }

  @Test
  void responseWithoutEmailAttributeIsRefusedEvenIfNameIdIsAnEmail() {
    assertThrows(AuthenticationException.class, () -> authenticate(SamlTestFixtures.response()
        .signedBy(idp).nameId(ADMIN_EMAIL).build()));

    verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    verify(userRepository, never()).save(any());
  }

  @Test
  void disabledIntegrationIsRefusedEvenWithValidResponse() {
    integration.setEnabled(false);

    assertThrows(AuthenticationException.class, () -> authenticate(SamlTestFixtures.response()
        .signedBy(idp).email("user@example.com").build()));

    verifyNoInteractions(userRepository);
  }

  // ---- role mapping --------------------------------------------------------------------------

  private UserRole roleOfCreatedUser(String... roleValues) {
    authenticate(SamlTestFixtures.response().signedBy(idp).email("new.user@example.com")
        .attribute(ROLES_ATTRIBUTE, roleValues).build());
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    return saved.getValue().getRole();
  }

  @ParameterizedTest
  @ValueSource(strings = {"non-admin", "badminton", "admin", "Admins", "administrators",
      "not ADMINISTRATOR", "ADMINISTRATOR,USER", "user"})
  void roleValueThatIsNotExactlyAdministratorMapsToUser(String roleValue) {
    assertEquals(UserRole.USER, roleOfCreatedUser(roleValue));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ADMINISTRATOR", "administrator", "Administrator", " Administrator "})
  void exactAdministratorRoleValueMapsToAdministrator(String roleValue) {
    assertEquals(UserRole.ADMINISTRATOR, roleOfCreatedUser(roleValue));
  }

  @Test
  void administratorAmongSeveralRoleValuesMapsToAdministrator() {
    assertEquals(UserRole.ADMINISTRATOR, roleOfCreatedUser("user", "administrator"));
  }

  @Test
  void severalNonAdministratorRoleValuesMapToUser() {
    assertEquals(UserRole.USER, roleOfCreatedUser("non-admin", "badminton"));
  }

  // ---- configurable administrator role values --------------------------------------------------

  private void configureAdminRoleValues(Object value) {
    integration.getParams().getParams()
        .put(SamlParameter.ADMIN_ROLE_VALUES.getParameterName(), value);
  }

  @Test
  void emptyAdminRoleValuesFallBackToDefault() {
    configureAdminRoleValues(List.of());
    assertEquals(UserRole.USER, roleOfCreatedUser("admin"));
  }

  @Test
  void emptyAdminRoleValuesStillAcceptDefaultAdministrator() {
    configureAdminRoleValues(List.of());
    assertEquals(UserRole.ADMINISTRATOR, roleOfCreatedUser("Administrator"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "ADMIN", " Admin ", "rp-admins"})
  void configuredAdminRoleValueGrantsAdministrator(String roleValue) {
    configureAdminRoleValues(List.of("admin", "rp-admins"));
    assertEquals(UserRole.ADMINISTRATOR, roleOfCreatedUser(roleValue));
  }

  @Test
  void configuredValuesReplaceTheDefault() {
    configureAdminRoleValues(List.of("admin"));
    assertEquals(UserRole.USER, roleOfCreatedUser("ADMINISTRATOR"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"non-admin", "admins", "administrator", "badmin", "admin,user"})
  void valueContainingConfiguredValueDoesNotGrantAdministrator(String roleValue) {
    configureAdminRoleValues(List.of("admin"));
    assertEquals(UserRole.USER, roleOfCreatedUser(roleValue));
  }

  @Test
  void secondValueOfMultiValuedAttributeCanGrantConfiguredAdministrator() {
    configureAdminRoleValues(List.of("rp-admins"));
    assertEquals(UserRole.ADMINISTRATOR, roleOfCreatedUser("staff", "RP-Admins"));
  }

  @Test
  void configuredAdminRoleValuesSurviveSaveAndReadThroughConverter() {
    Integration target = new Integration();
    target.setEnabled(true);
    UpdateAuthRQ request = samlRequest(Map.of(
        SamlParameter.ADMIN_ROLE_VALUES.getParameterName(),
        List.of(" admin ", "rp-admins", "ADMIN")));

    SamlConverter.UPDATE_FROM_REQUEST.accept(request, target);

    assertEquals(List.of("admin", "rp-admins"), SamlConverter.TO_RESOURCE.apply(target)
        .getAdminRoleValues());
  }

  @Test
  void absentAdminRoleValuesReadBackAsDefaultAndEmptyListClearsStoredValues() {
    Integration target = new Integration();
    target.setEnabled(true);
    SamlConverter.UPDATE_FROM_REQUEST.accept(samlRequest(Map.of()), target);
    assertEquals(List.of("ADMINISTRATOR"),
        SamlConverter.TO_RESOURCE.apply(target).getAdminRoleValues());

    SamlConverter.UPDATE_FROM_REQUEST.accept(samlRequest(Map.of(
        SamlParameter.ADMIN_ROLE_VALUES.getParameterName(), List.of("admin"))), target);
    SamlConverter.UPDATE_FROM_REQUEST.accept(samlRequest(Map.of()), target);
    assertEquals(List.of("admin"), SamlConverter.TO_RESOURCE.apply(target).getAdminRoleValues());

    SamlConverter.UPDATE_FROM_REQUEST.accept(samlRequest(Map.of(
        SamlParameter.ADMIN_ROLE_VALUES.getParameterName(), List.of())), target);
    assertEquals(List.of("ADMINISTRATOR"),
        SamlConverter.TO_RESOURCE.apply(target).getAdminRoleValues());
  }

  private static UpdateAuthRQ samlRequest(Map<String, Object> extra) {
    Map<String, Object> params = new HashMap<>(extra);
    params.put(SamlParameter.IDP_NAME.getParameterName(), REGISTRATION_ID);
    params.put(SamlParameter.IDP_METADATA_URL.getParameterName(), "https://idp.example.com/meta");
    params.put(SamlParameter.EMAIL_ATTRIBUTE.getParameterName(), EMAIL_ATTRIBUTE);
    params.put(SamlParameter.FULL_NAME_ATTRIBUTE.getParameterName(), "name");
    UpdateAuthRQ request = new UpdateAuthRQ();
    request.setEnabled(true);
    request.setIntegrationParams(params);
    return request;
  }

  private static void validate(Object adminRoleValues) {
    new SamlUpdateAuthRequestValidator(new SamlRequiredParamNamesProvider()).validate(
        samlRequest(Map.of(SamlParameter.ADMIN_ROLE_VALUES.getParameterName(), adminRoleValues)));
  }

  @Test
  void validRequestWithAdminRoleValuesPassesValidation() {
    validate(List.of("admin", " rp-admins "));
    validate(List.of());
  }

  @Test
  void blankAdminRoleValueIsRejected() {
    assertThrows(ReportPortalException.class, () -> validate(List.of("admin", "  ")));
    assertThrows(ReportPortalException.class, () -> validate(List.of("")));
  }

  @Test
  void nonListAndNonStringAdminRoleValuesAreRejected() {
    assertThrows(ReportPortalException.class, () -> validate("admin"));
    assertThrows(ReportPortalException.class, () -> validate(List.of(1)));
  }

  @Test
  void tooManyOrTooLongAdminRoleValuesAreRejected() {
    assertThrows(ReportPortalException.class, () -> validate(
        java.util.stream.IntStream.range(0, SamlParameter.MAX_ADMIN_ROLE_VALUES + 1)
            .mapToObj(i -> "role" + i).toList()));
    assertThrows(ReportPortalException.class, () -> validate(
        List.of("x".repeat(SamlParameter.MAX_ADMIN_ROLE_VALUE_LENGTH + 1))));
    validate(List.of("x".repeat(SamlParameter.MAX_ADMIN_ROLE_VALUE_LENGTH)));
  }
}
