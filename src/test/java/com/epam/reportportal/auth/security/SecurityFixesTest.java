package com.epam.reportportal.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.reportportal.auth.AdminPasswordInitializer;
import com.epam.reportportal.auth.TokenServicesFacade;
import com.epam.reportportal.auth.commons.ReportPortalUser;
import com.epam.reportportal.auth.config.utils.JwtReportPortalUserConverter;
import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.dao.UserRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.oauth.OAuthRegistration;
import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.entity.user.UserRole;
import com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters;
import com.epam.reportportal.auth.integration.github.GitHubClient;
import com.epam.reportportal.auth.integration.github.GitHubOAuth2UserService;
import com.epam.reportportal.auth.integration.github.GitHubUserReplicator;
import com.epam.reportportal.auth.integration.handler.impl.CreateAuthIntegrationHandlerImpl;
import com.epam.reportportal.auth.integration.ldap.LdapAuthProvider;
import com.epam.reportportal.auth.model.settings.OAuthRegistrationResource;
import com.epam.reportportal.auth.store.MutableClientRegistrationRepository;
import com.epam.reportportal.auth.util.AuthUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

/** Plain unit tests (no Spring context, no database) for the security fixes. */
class SecurityFixesTest {

  // ---- token lifetime ----

  private Jwt capturedJwt(TokenServicesFacade facade, JwtEncoder encoder) {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").claim("a", "b").build();
    when(encoder.encode(any())).thenReturn(jwt);
    Authentication auth = new UsernamePasswordAuthenticationToken("u", null,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
    facade.createToken("ui", "u", auth.getAuthorities(), Map.of());
    facade.createToken(com.epam.reportportal.auth.ReportPortalClient.ui, "u", auth, Map.of());
    return jwt;
  }

  private List<Duration> lifetimes(int configured) {
    JwtEncoder encoder = mock(JwtEncoder.class);
    TokenServicesFacade facade = new TokenServicesFacade(encoder, "iss", configured);
    capturedJwt(facade, encoder);
    ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
    org.mockito.Mockito.verify(encoder, org.mockito.Mockito.times(2)).encode(captor.capture());
    return captor.getAllValues().stream().map(p -> {
      var claims = p.getClaims();
      return Duration.between(claims.getIssuedAt(), claims.getExpiresAt());
    }).toList();
  }

  @Test
  void tokenLifetimeUsesConfiguredValidityInBothMethods() {
    assertEquals(List.of(Duration.ofSeconds(600), Duration.ofSeconds(600)), lifetimes(600));
  }

  @Test
  void tokenLifetimeDefaultsToOneDay() {
    assertEquals(List.of(Duration.ofDays(1), Duration.ofDays(1)), lifetimes(86400));
    assertEquals(List.of(Duration.ofDays(1), Duration.ofDays(1)), lifetimes(0));
  }

  // ---- converter ----

  private static UserDetails principal(boolean active, UserRole role) {
    return ReportPortalUser.userBuilder()
        .withUserDetails(org.springframework.security.core.userdetails.User.builder()
            .username("u").password("").disabled(!active)
            .authorities(new SimpleGrantedAuthority(role.getAuthority())).build())
        .withUserRole(role).build();
  }

  private static Jwt jwtWithAuthorities(String... authorities) {
    return Jwt.withTokenValue("t").header("alg", "HS256").claim("user_name", "u")
        .claim("authorities", List.of(authorities)).issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60)).build();
  }

  @Test
  void converterTakesRoleFromDatabaseNotFromToken() {
    UserDetailsService uds = name -> principal(true, UserRole.USER);
    var token = new JwtReportPortalUserConverter(uds).convert(
        jwtWithAuthorities("ROLE_ADMINISTRATOR"));
    List<String> roles = token.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .toList();
    assertEquals(List.of("ROLE_USER"), roles);
  }

  @Test
  void converterRejectsInactiveUser() {
    UserDetailsService uds = name -> principal(false, UserRole.USER);
    assertThrows(DisabledException.class,
        () -> new JwtReportPortalUserConverter(uds).convert(jwtWithAuthorities("ROLE_USER")));
  }

  // ---- deactivated users ----

  @Test
  void requireActiveRejectsInactiveAndNull() {
    User user = new User();
    user.setActive(false);
    assertThrows(DisabledException.class, () -> AuthUtils.requireActive(user));
    user.setActive(null);
    assertThrows(DisabledException.class, () -> AuthUtils.requireActive(user));
    user.setActive(true);
    assertSame(user, AuthUtils.requireActive(user));
  }

  // ---- LDAP enabled flag ----

  @Test
  void ldapDisabledIntegrationIsNotEnabled() {
    IntegrationRepository repo = mock(IntegrationRepository.class);
    Integration integration = new Integration();
    when(repo.findAllByTypeIn("ldap")).thenReturn(List.of(integration));
    LdapAuthProvider provider = new LdapAuthProvider(repo, null, null, null);

    integration.setEnabled(false);
    assertFalse((Boolean) ReflectionTestUtils.invokeMethod(provider, "isEnabled"));
    integration.setEnabled(true);
    assertTrue((Boolean) ReflectionTestUtils.invokeMethod(provider, "isEnabled"));
  }

  // ---- GitHub organization restriction ----

  // ---- client secret ----

  @Test
  void clientSecretIsNotSerialized() throws Exception {
    OAuthRegistration db = new OAuthRegistration();
    ReflectionTestUtils.setField(db, "id", "github");
    ReflectionTestUtils.setField(db, "clientId", "cid");
    ReflectionTestUtils.setField(db, "clientSecret", "s3cret");
    OAuthRegistrationResource resource = OAuthRegistrationConverters.TO_RESOURCE.apply(db);
    String json = new ObjectMapper().writeValueAsString(resource);
    assertFalse(json.contains("s3cret"));
    assertFalse(json.contains("clientSecret"));

    OAuthRegistrationResource in = new ObjectMapper().readValue(
        "{\"clientId\":\"c\",\"clientSecret\":\"abc\"}", OAuthRegistrationResource.class);
    assertEquals("abc", in.getClientSecret());
  }

  @Test
  void updateWithoutSecretKeepsStoredSecret() {
    MutableClientRegistrationRepository repo = mock(MutableClientRegistrationRepository.class);
    OAuthRegistration stored = new OAuthRegistration();
    ReflectionTestUtils.setField(stored, "id", "github");
    ReflectionTestUtils.setField(stored, "clientSecret", "stored-secret");
    when(repo.findOAuthRegistrationById("github")).thenReturn(Optional.of(stored));
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    var handler = new CreateAuthIntegrationHandlerImpl(repo, null, null);
    ReflectionTestUtils.setField(handler, "pathValue", "/");

    OAuthRegistrationResource rq = new OAuthRegistrationResource();
    rq.setClientId("new-client-id");
    handler.createOrUpdateOauthSettings("github", rq);

    ArgumentCaptor<OAuthRegistration> saved = ArgumentCaptor.forClass(OAuthRegistration.class);
    verify(repo).save(saved.capture());
    assertEquals("stored-secret", saved.getValue().getClientSecret());
    assertEquals("new-client-id", saved.getValue().getClientId());
  }

  @Test
  void createWithoutSecretIsRejected() {
    MutableClientRegistrationRepository repo = mock(MutableClientRegistrationRepository.class);
    when(repo.findOAuthRegistrationById("github")).thenReturn(Optional.empty());
    var handler = new CreateAuthIntegrationHandlerImpl(repo, null, null);
    OAuthRegistrationResource rq = new OAuthRegistrationResource();
    rq.setClientId("cid");
    assertThrows(com.epam.reportportal.auth.rules.exception.ReportPortalException.class,
        () -> handler.createOrUpdateOauthSettings("github", rq));
  }

  // ---- admin password initializer ----

  private AdminPasswordInitializer initializer(UserRepository repo, String password) {
    var init = new AdminPasswordInitializer(repo);
    ReflectionTestUtils.setField(init, "adminPassword", password);
    return init;
  }

  @Test
  void missingLastLoginIsTreatedAsNeverLoggedIn() {
    UserRepository repo = mock(UserRepository.class);
    User admin = new User();
    admin.setPassword("$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234");
    admin.setMetadata(null);
    when(repo.findByLogin("admin@reportportal.internal")).thenReturn(Optional.of(admin));

    // no password configured: enforcement must trigger
    assertThrows(com.epam.reportportal.auth.exception.EnvironmentVariablesNotProvidedException.class,
        () -> initializer(repo, "").run());
    // password configured: it must be applied
    initializer(repo, "newpass").run();
    verify(repo).save(admin);
  }
}
