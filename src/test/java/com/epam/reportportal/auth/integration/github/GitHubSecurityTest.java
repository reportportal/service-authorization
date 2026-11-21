package com.epam.reportportal.auth.integration.github;

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


/** Unit tests for GitHub login security fixes. */
class GitHubSecurityTest {

  @Test
  void githubReplicatorRejectsInactiveExistingUser() {
    UserRepository repo = mock(UserRepository.class);
    User existing = new User();
    existing.setActive(false);
    existing.setLogin("a@b.c");
    existing.setUserType(com.epam.reportportal.auth.entity.user.UserType.GITHUB);
    when(repo.findByEmail("a@b.c")).thenReturn(Optional.of(existing));
    GitHubUserReplicator replicator = new GitHubUserReplicator(repo, null, null, null, null, null);
    var gh = new UserResource();
    gh.setEmail("a@b.c");
    assertThrows(DisabledException.class, () -> replicator.replicateUser(gh, mock(GitHubClient.class)));
    verify(repo, never()).save(any());
  }

  private static OAuth2UserRequest githubRequest() {
    ClientRegistration reg = ClientRegistration.withRegistrationId("github").clientId("c")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://x").authorizationUri("http://x/a").tokenUri("http://x/t")
        .userInfoUri("http://x/u").userNameAttributeName("id").build();
    return new OAuth2UserRequest(reg,
        new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "tok", Instant.now(),
            Instant.now().plusSeconds(60)));
  }

  @Test
  void githubLoginFailsClosedWhenRegistrationMissing() {
    GitHubUserReplicator replicator = mock(GitHubUserReplicator.class);
    Supplier<OAuthRegistrationResource> missing = () -> {
      throw new OAuth2AuthenticationException("missing");
    };
    GitHubClient client = mock(GitHubClient.class);
    when(client.getUser()).thenReturn(new UserResource());
    try (MockedStatic<GitHubClient> st = mockStatic(GitHubClient.class)) {
      st.when(() -> GitHubClient.withAccessToken("tok")).thenReturn(client);
      var service = new GitHubOAuth2UserService(replicator, missing);
      assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(githubRequest()));
      var nullService = new GitHubOAuth2UserService(replicator, () -> null);
      assertThrows(OAuth2AuthenticationException.class, () -> nullService.loadUser(githubRequest()));
    }
    verify(replicator, never()).replicateUser(any(), any());
  }

  @Test
  void githubOrganizationRestrictionIsReadOnEachLogin() {
    GitHubUserReplicator replicator = mock(GitHubUserReplicator.class);
    OAuthRegistrationResource registration = new OAuthRegistrationResource();
    GitHubClient client = mock(GitHubClient.class);
    var ghUser = new UserResource();
    ghUser.setLogin("bob");
    when(client.getUser()).thenReturn(ghUser);
    when(client.getUserOrganizations("bob")).thenReturn(List.of());
    when(replicator.replicateUser(any(), any())).thenReturn(
        ReportPortalUser.userBuilder().withUserDetails(
            org.springframework.security.core.userdetails.User.builder().username("bob")
                .password("").authorities("ROLE_USER").build()).build());
    try (MockedStatic<GitHubClient> st = mockStatic(GitHubClient.class)) {
      st.when(() -> GitHubClient.withAccessToken("tok")).thenReturn(client);
      var service = new GitHubOAuth2UserService(replicator, () -> registration);
      // no restriction yet: login allowed
      service.loadUser(githubRequest());
      // admin configures a restriction afterwards: the same service instance must enforce it
      registration.setRestrictions(Map.of("organizations", "acme"));
      assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(githubRequest()));
    }
  }

}
