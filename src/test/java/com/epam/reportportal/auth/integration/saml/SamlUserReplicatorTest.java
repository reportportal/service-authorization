package com.epam.reportportal.auth.integration.saml;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SamlUserReplicatorTest {

    @InjectMocks
    private SamlUserReplicator samlUserReplicator;

    @Test
    public void test() {
        ReportPortalSamlAuthentication samlAuthentication = new ReportPortalSamlAuthentication(false, null, null, null, null);

        samlUserReplicator.replicateUser(samlAuthentication);
    }
}