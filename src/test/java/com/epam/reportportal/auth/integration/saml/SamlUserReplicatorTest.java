package com.epam.reportportal.auth.integration.saml;

import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.database.entity.user.UserType;
import com.epam.ta.reportportal.database.personal.PersonalProjectService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.saml2.authentication.Issuer;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.saml2.metadata.NameId;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SamlUserReplicatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServerSettingsRepository serverSettingsRepository;

    @Mock
    private ProjectRepository projectRepository;

    private SamlUserReplicator underTest;

    private PersonalProjectService projectService;
    private User user;
    private ServerSettings serverSettings;
    private ReportPortalSamlAuthentication authentication;
    private long defaultTimestamp;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        projectService = new PersonalProjectService(projectRepository);
        underTest = new SamlUserReplicator(userRepository, projectRepository, projectService, null, serverSettingsRepository);
        user = createTestUser();
        serverSettings = createServerSettings();
        authentication = createAuthentication();

        when(userRepository.findOne("tst")).thenReturn(null);
        when(serverSettingsRepository.findOne("default")).thenReturn(serverSettings);
    }

    @Test
    public void replicateUser_whenUserIsNotExist_shouldSaveUser() {
        user = underTest.replicateUser(authentication);

        verify(userRepository, atLeastOnce()).findOne("tst");
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    public void replicateUser_whenUserIsNotExist_shouldCallServerSettings() {
        underTest.replicateUser(authentication);

        verify(serverSettingsRepository, atLeastOnce()).findOne("default");
    }

    @Test
    public void replicateUser_whenUserIsNotExist_and_HasNonEmptyFullNameAttributeInConfiguration_shouldUseFullName() {
        authentication = createAuthentication(createAssertion(createNonDefaultAttributes()));
        serverSettings.getSamlProviderDetails().values().forEach(details -> {
            details.setFullNameAttributeId("Full_Name");
            details.setEmailAttributeId("Email_attr");
        });

        User result = underTest.replicateUser(authentication);

        assertEquals("Test FullName User", result.getFullName());
    }

    @Test
    public void replicateUser_whenUserIsNotExist_and_HasEmptyFullNameAttributeInConfiguration_shouldConcatenateName() {
        authentication = createAuthentication(createAssertion(createNonDefaultAttributes()));
        serverSettings.getSamlProviderDetails().values().forEach(details -> {
            details.setLastNameAttributeId("Last_Name");
            details.setFirstNameAttributeId("First_Name");
            details.setEmailAttributeId("Email_attr");
        });

        User result = underTest.replicateUser(authentication);

        assertEquals("Test User", result.getFullName());
    }

    @Test
    public void replicateUser_whenUserIsNotExist_and_ConfigurationForIssuerNotFound_shouldUseDefaultAttributesForFullNameAndEmail() {
        serverSettings.getSamlProviderDetails().values().forEach(details -> details.setIdpUrl("http://test"));

        User result = underTest.replicateUser(authentication);

        assertEquals("Test User", result.getFullName());
        assertEquals("test.user@example.com", result.getEmail());
    }

    @Test
    public void replicateUser_whenUserIsNotExist_shouldFillDefaultParameters() {
        User result = underTest.replicateUser(authentication);

        assertEquals("tst", result.getLogin());
        assertEquals(UserType.SAML, result.getType());
        assertEquals(UserRole.USER, result.getRole());
        assertFalse(result.getIsExpired());
        assertNull(result.getPassword());
        assertNull(result.getPhotoId());
        assertTrue(System.currentTimeMillis() - result.getMetaInfo().getLastLogin().getTime() < 1000);
        assertTrue(System.currentTimeMillis() - result.getMetaInfo().getSynchronizationDate().getTime() < 1000);
        assertEquals("tst_personal", result.getDefaultProject());
    }

    @Test
    public void replicateUser_whenUserIsNotExist_and_ConfigurationNotFound_shouldConcatenateName() {}

    @Test
    public void replicateUser_whenUserIsExist_shouldReturnExistUser() {
        when(userRepository.findOne("tst")).thenReturn(user);

        User result = underTest.replicateUser(authentication);

        assertEquals(user, result);
    }

    @Test
    public void replicateUser_whenUserIsExist_mustNotSaveUser() {
        when(userRepository.findOne("tst")).thenReturn(user);

        underTest.replicateUser(authentication);

        verify(userRepository, never()).save(any(User.class));
    }

    private ReportPortalSamlAuthentication createAuthentication() {
        Assertion assertion = createAssertion(createDefaultAttributes());
        return createAuthentication(assertion);
    }

    private ReportPortalSamlAuthentication createAuthentication(Assertion assertion) {
        return new ReportPortalSamlAuthentication(true, assertion, "entityId", "holdingEntity", "relayState");
    }

    private Assertion createAssertion(List<Attribute> attributes) {
        Assertion assertion = new Assertion();
        org.springframework.security.saml.saml2.authentication.Subject subject =
                new org.springframework.security.saml.saml2.authentication.Subject();
        NameIdPrincipal principal = new NameIdPrincipal();
        principal.setFormat(NameId.EMAIL);
        principal.setValue("tst");
        subject.setPrincipal(principal);
        Issuer issuer = new Issuer();
        issuer.setValue("http://test/saml/provider");
        assertion.setSubject(subject);
        assertion.setIssuer(issuer);
        assertion.setAttributes(attributes);
        return assertion;
    }

    private List<Attribute> createNonDefaultAttributes() {
        Attribute email = new Attribute();
        email.setName("Email_attr");
        email.setValues(Collections.singletonList("test.user@example.com"));
        Attribute fullName = new Attribute();
        fullName.setName("Full_Name");
        fullName.setValues(Collections.singletonList("Test FullName User"));
        Attribute firstName = new Attribute();
        firstName.setName("First_Name");
        firstName.setValues(Collections.singletonList("Test"));
        Attribute lastName = new Attribute();
        lastName.setName("Last_Name");
        lastName.setValues(Collections.singletonList("User"));
        return Arrays.asList(email, fullName, firstName, lastName);
    }

    private List<Attribute> createDefaultAttributes() {
        Attribute email = new Attribute();
        email.setName("Email");
        email.setValues(Collections.singletonList("test.user@example.com"));
        Attribute fullName = new Attribute();
        fullName.setName("FullName");
        fullName.setValues(Collections.singletonList("Test FullName User"));
        Attribute firstName = new Attribute();
        firstName.setName("FirstName");
        firstName.setValues(Collections.singletonList("Test"));
        Attribute lastName = new Attribute();
        lastName.setName("LastName");
        lastName.setValues(Collections.singletonList("User"));
        return Arrays.asList(email, fullName, firstName, lastName);
    }

    private ServerSettings createServerSettings() {
        ServerSettings serverSettings = new ServerSettings();
        SamlProviderDetails details = new SamlProviderDetails();
        details.setIdpUrl("http://test/saml/provider");
        details.setFirstNameAttributeId("FirstName");
        details.setLastNameAttributeId("LastName");
        details.setEmailAttributeId("Email");
        serverSettings.setSamlProviderDetails(Collections.singletonMap("test", details));
        return serverSettings;
    }

    private User createTestUser() {
        User user = new User();
        user.setFullName("Test user");
        user.setEmail("test.user@example.com");
        user.setLogin("tst");
        user.setType(UserType.INTERNAL);
        user.setRole(UserRole.USER);
        user.setIsExpired(false);
        user.setDefaultProject("defaultProject");
        user.setMetaInfo(createMetaInfo());
        return user;
    }

    private User.MetaInfo createMetaInfo() {
        User.MetaInfo meta = new User.MetaInfo();
        Date now = new Date(1559469600000L);
        meta.setLastLogin(now);
        meta.setSynchronizationDate(now);
        return meta;
    }

}