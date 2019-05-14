package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.AuthUtils;
import com.epam.ta.reportportal.database.entity.user.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;

public class ReportPortalSamlAuthenticationManager implements AuthenticationManager {

    private SamlUserReplicator samlUserReplicator;

    public ReportPortalSamlAuthenticationManager(SamlUserReplicator samlUserReplicator) {
        this.samlUserReplicator = samlUserReplicator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof DefaultSamlAuthentication) {
            ReportPortalSamlAuthentication reportPortalSamlAuthentication =
                    new ReportPortalSamlAuthentication((DefaultSamlAuthentication) authentication);
            if (reportPortalSamlAuthentication.isAuthenticated()) {
                User user = samlUserReplicator.replicateUser(reportPortalSamlAuthentication);

                reportPortalSamlAuthentication.setAuthorities(AuthUtils.AS_AUTHORITIES.apply(user.getRole()));

                SecurityContextHolder.getContext().setAuthentication(reportPortalSamlAuthentication);
            }
            return reportPortalSamlAuthentication;
        }
        return authentication;
    }
}
