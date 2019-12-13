package com.epam.reportportal.auth.integration.saml;

import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.dao.AttributeRepository;
import com.epam.ta.reportportal.dao.IssueTypeRepository;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.SamlProviderDetailsRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.saml.SamlProviderDetails;
import com.epam.ta.reportportal.entity.user.ProjectUser;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.util.PersonalProjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.saml2.authentication.Issuer;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.saml2.authentication.Subject;
import org.springframework.security.saml.saml2.metadata.NameId;

import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SamlUserReplicatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SamlProviderDetailsRepository samlProviderDetailsRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserBinaryDataService userBinaryDataService;

    @Mock
    private AttributeRepository attributeRepository;
    @Mock
    private IssueTypeRepository issueTypeRepository;

    private SamlUserReplicator samlUserReplicator;

    @Before
    public void before(){
        PersonalProjectService personalProjectService = new PersonalProjectService(projectRepository, attributeRepository, issueTypeRepository);
        samlUserReplicator = new SamlUserReplicator(userRepository, projectRepository, personalProjectService, userBinaryDataService, samlProviderDetailsRepository);
    }

    @Test
    public void userIsNotPresentButSamlInfoIsPresentSoUserIsCreated() {
        String issuerUrl = "www.google.com";

        Assertion assertion = getAssertion();
        SamlProviderDetails samlProviderDetails = getSamlProviderDetails(issuerUrl);

        Project project = new Project(1L, "p");
        project.setUsers(asSet(new ProjectUser()));

        when(projectRepository.findByName(any())).thenReturn(Optional.of(project));
        when(samlProviderDetailsRepository.findAll()).thenReturn(singletonList(samlProviderDetails));

        ReportPortalSamlAuthentication samlAuthentication = new ReportPortalSamlAuthentication(false, assertion, null, null, null);
        samlAuthentication.setIssuer(issuerUrl);

        User user = samlUserReplicator.replicateUser(samlAuthentication);

        assertThat(user.getLogin()).isEqualTo("test");
        assertThat(user.getEmail()).isEqualTo("test@test.nl");
    }

    private SamlProviderDetails getSamlProviderDetails(String issuerUrl) {
        SamlProviderDetails samlProviderDetails = new SamlProviderDetails();
        samlProviderDetails.setIdpUrl(issuerUrl);
        samlProviderDetails.setEmailAttributeId("email");
        return samlProviderDetails;
    }

    private Assertion getAssertion() {
        NameIdPrincipal principal = new NameIdPrincipal().setFormat(NameId.EMAIL).setValue("test@test.nl");
        Issuer issuer = new Issuer().setValue("value");
        Subject subject = new Subject().setPrincipal(principal);
        return new Assertion().setIssuer(issuer).setSubject(subject).setAttributes(asList(new Attribute().setName("email").setValues(asList("test@test.nl"))));
    }
}