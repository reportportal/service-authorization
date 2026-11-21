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

package com.epam.reportportal.auth.config.saml;

import static com.epam.reportportal.auth.integration.saml.SamlTestFixtures.REGISTRATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.reportportal.auth.AuthFailureHandler;
import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.entity.user.UserRole;
import com.epam.reportportal.auth.entity.user.UserType;
import com.epam.reportportal.auth.integration.saml.ReloadableRelyingPartyRegistrationRepository;
import com.epam.reportportal.auth.integration.saml.SamlAuthSuccessHandler;
import com.epam.reportportal.auth.integration.saml.SamlResponseAuthenticationConverter;
import com.epam.reportportal.auth.integration.saml.SamlTestFixtures;
import com.epam.reportportal.auth.integration.saml.SamlTestFixtures.Identity;
import com.epam.reportportal.auth.integration.saml.SamlUserReplicator;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Exercises the real SAML security filter chain of {@link Saml2AuthenticationConfiguration} over
 * HTTP (without database): whatever is POSTed to the assertion consumer URL must pass response
 * validation before the user replicator or the success handler (which mints the token) is reached.
 */
@SpringJUnitWebConfig(classes = SamlAssertionConsumerEndpointTest.Config.class)
class SamlAssertionConsumerEndpointTest {

  private static final Identity IDP = Identity.generate("idp");
  private static final Identity ATTACKER = Identity.generate("attacker");
  private static final String ACS_URL = "/login/saml2/sso/" + REGISTRATION_ID;
  private static final String ADMIN_EMAIL = "admin@example.com";

  // Deliberately not annotated with @Configuration (or any other stereotype): the full-context
  // tests component-scan com.epam.reportportal and must not pick these beans up.
  @EnableWebMvc
  @EnableWebSecurity
  @Import(Saml2AuthenticationConfiguration.class)
  static class Config {

    @Bean
    SamlUserReplicator samlUserReplicator() {
      return mock(SamlUserReplicator.class);
    }

    @Bean
    SamlResponseAuthenticationConverter samlResponseAuthenticationConverter(
        SamlUserReplicator samlUserReplicator) {
      return new SamlResponseAuthenticationConverter(samlUserReplicator);
    }

    @Bean
    SamlAuthSuccessHandler samlAuthSuccessHandler() {
      return mock(SamlAuthSuccessHandler.class);
    }

    @Bean
    AuthFailureHandler authFailureHandler() {
      return new AuthFailureHandler();
    }

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
      // same ACS template as RelyingPartyBuilder
      return new ReloadableRelyingPartyRegistrationRepository(SamlTestFixtures.registration(
          "{baseUrl}/login/saml2/sso/{registrationId}", IDP.certificate()));
    }
  }

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private SamlUserReplicator samlUserReplicator;

  @Autowired
  private SamlAuthSuccessHandler successHandler;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    reset(samlUserReplicator, successHandler);
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
        .build();
  }

  private MvcResult postResponse(String url, String responseXml) throws Exception {
    return mockMvc.perform(post(url).param("SAMLResponse",
            Base64.getEncoder().encodeToString(responseXml.getBytes(StandardCharsets.UTF_8))))
        .andReturn();
  }

  private void assertLoginFailed(MvcResult result) {
    assertEquals(302, result.getResponse().getStatus());
    String location = result.getResponse().getRedirectedUrl();
    assertTrue(location != null && location.contains("ui/#login") && location.contains(
        "errorAuth="), location);
    verifyNoInteractions(samlUserReplicator, successHandler);
  }

  @Test
  void handWrittenUnsignedResponseNamingAdministratorIsRejected() throws Exception {
    String forged = SamlTestFixtures.response().nameId(ADMIN_EMAIL).email(ADMIN_EMAIL)
        .attribute(SamlTestFixtures.ROLES_ATTRIBUTE, "admin").build();

    assertLoginFailed(postResponse(ACS_URL, forged));
  }

  @Test
  void responseSignedWithWrongKeyIsRejected() throws Exception {
    String forged = SamlTestFixtures.response().signedBy(ATTACKER).nameId(ADMIN_EMAIL)
        .email(ADMIN_EMAIL).build();

    assertLoginFailed(postResponse(ACS_URL, forged));
  }

  @Test
  void doctypePayloadIsRejected() throws Exception {
    String payload = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/hostname\">]>"
        + "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"_x\""
        + " Version=\"2.0\">&xxe;</saml2p:Response>";

    assertLoginFailed(postResponse(ACS_URL, payload));
  }

  @Test
  void validResponseForUnknownRegistrationIsRejected() throws Exception {
    String valid = SamlTestFixtures.response().signedBy(IDP).email("user@example.com")
        .destination("http://localhost/login/saml2/sso/unknown-idp").build();

    assertLoginFailed(postResponse("/login/saml2/sso/unknown-idp", valid));
  }

  @Test
  void correctlySignedResponseReachesSuccessHandlerAsReplicatedUser() throws Exception {
    User user = new User();
    user.setLogin("user@example.com");
    user.setEmail("user@example.com");
    user.setRole(UserRole.USER);
    user.setUserType(UserType.SAML);
    user.setActive(true);
    when(samlUserReplicator.replicateUser(eq(REGISTRATION_ID), anyMap())).thenReturn(user);

    String valid = SamlTestFixtures.response().signedBy(IDP).email("user@example.com").build();
    mockMvc.perform(post(ACS_URL).param("SAMLResponse",
            Base64.getEncoder().encodeToString(valid.getBytes(StandardCharsets.UTF_8))))
        .andExpect(status().isOk());

    ArgumentCaptor<Authentication> authentication = ArgumentCaptor.forClass(Authentication.class);
    verify(successHandler).onAuthenticationSuccess(any(), any(), authentication.capture());
    assertTrue(authentication.getValue() instanceof Saml2Authentication);
    assertEquals("user@example.com", authentication.getValue().getName());
    assertEquals(List.of(UserRole.USER.getAuthority()),
        authentication.getValue().getAuthorities().stream().map(Object::toString).toList());
  }

  @Test
  void assertionConsumerUrlIsServedByExactlyOneSamlFilter() {
    FilterChainProxy proxy = context.getBean("springSecurityFilterChain", FilterChainProxy.class);

    long samlFilters = proxy.getFilterChains().stream()
        .flatMap(chain -> chain.getFilters().stream())
        .filter(Saml2WebSsoAuthenticationFilter.class::isInstance)
        .count();

    assertEquals(1, samlFilters,
        "A second Saml2WebSsoAuthenticationFilter could bypass response validation");
  }

  @Test
  void testConfigurationIsInvisibleToApplicationComponentScan() {
    // The full-context tests scan com.epam.reportportal (see TestConfig); this class' Config must
    // not be a candidate, otherwise its beans clash with the application's.
    var scanner = new ClassPathScanningCandidateComponentProvider(true);

    var candidates = scanner.findCandidateComponents("com.epam.reportportal").stream()
        .map(BeanDefinition::getBeanClassName)
        .toList();

    assertFalse(candidates.isEmpty());
    assertFalse(candidates.contains(Config.class.getName()), candidates.toString());
  }
}
