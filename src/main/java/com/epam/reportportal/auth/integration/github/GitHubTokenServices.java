/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.integration.github;

import com.epam.reportportal.auth.AuthUtils;
import com.epam.ta.reportportal.database.entity.settings.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.user.User;
import com.google.common.base.Splitter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Token services for GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class GitHubTokenServices implements ResourceServerTokenServices {

    private final GitHubUserReplicator replicator;
    private final Supplier<OAuth2LoginDetails> loginDetails;

    public GitHubTokenServices(GitHubUserReplicator replicatingPrincipalExtractor, Supplier<OAuth2LoginDetails> loginDetails) {
        this.replicator = replicatingPrincipalExtractor;
        this.loginDetails = loginDetails;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
        UserResource gitHubUser = gitHubClient.getUser();

        List<String> allowedOrganizations = ofNullable(loginDetails.get().getRestrictions())
                .flatMap(restrictions -> ofNullable(restrictions.get("organizations")))
                .map(it -> Splitter.on(",").omitEmptyStrings().splitToList(it))
                .orElse(emptyList());
        if (!allowedOrganizations.isEmpty()) {
            boolean assignedToOrganization = gitHubClient.getUserOrganizations(gitHubUser).stream().map(userOrg -> userOrg.login)
                    .anyMatch(allowedOrganizations::contains);
            if (!assignedToOrganization) {
                throw new InsufficientOrganizationException("User '" + gitHubUser.login + "' does not belong to allowed GitHUB organization");
            }
        }

        User user = replicator.replicateUser(gitHubUser, gitHubClient);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getId(), "N/A",
                AuthUtils.AS_AUTHORITIES.apply(user.getRole()));

        Map<String, Serializable> extensionProperties = Collections.singletonMap("upstream_token", accessToken);
        OAuth2Request request = new OAuth2Request(null, loginDetails.get().getClientId(), null, true, null, null, null, null, extensionProperties);
        return new OAuth2Authentication(request, token);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }

    public static class InsufficientOrganizationException extends AuthenticationException {

        public InsufficientOrganizationException(String msg) {
            super(msg);
        }
    }

}
