/*
 * Copyright 2019 EPAM Systems
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
package com.epam.reportportal.auth.integration.github;

import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
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

import static com.epam.reportportal.auth.integration.github.ExternalOauth2TokenConverter.UPSTREAM_TOKEN;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Token services for GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class GitHubTokenServices implements ResourceServerTokenServices {

    private final GitHubUserReplicator replicator;
    private final Supplier<OAuthRegistrationResource> oAuthRegistrationSupplier;

    public GitHubTokenServices(GitHubUserReplicator replicatingPrincipalExtractor,
            Supplier<OAuthRegistrationResource> oAuthRegistrationSupplier) {
        this.replicator = replicatingPrincipalExtractor;
        this.oAuthRegistrationSupplier = oAuthRegistrationSupplier;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
        UserResource gitHubUser = gitHubClient.getUser();

        OAuthRegistrationResource oAuthRegistrationResource = oAuthRegistrationSupplier.get();
        List<String> allowedOrganizations = ofNullable(oAuthRegistrationResource.getRestrictions())
                .flatMap(restrictions -> ofNullable(restrictions.get("organizations")))
                .map(it -> Splitter.on(",").omitEmptyStrings().splitToList(it))
                .orElse(emptyList());
        if (!allowedOrganizations.isEmpty()) {
            boolean assignedToOrganization = gitHubClient.getUserOrganizations(gitHubUser).stream().map(userOrg -> userOrg.login)
                    .anyMatch(allowedOrganizations::contains);
            if (!assignedToOrganization) {
                throw new InsufficientOrganizationException("User '" + gitHubUser.getLogin() + "' does not belong to allowed GitHUB organization");
            }
        }

        User user = replicator.replicateUser(gitHubUser, gitHubClient);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getLogin(), "N/A",
                AuthUtils.AS_AUTHORITIES.apply(user.getRole()));

        Map<String, Serializable> extensionProperties = Collections.singletonMap(UPSTREAM_TOKEN, accessToken);
        OAuth2Request request = new OAuth2Request(null, oAuthRegistrationResource.getClientId(), null, true, null, null, null, null, extensionProperties);
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
