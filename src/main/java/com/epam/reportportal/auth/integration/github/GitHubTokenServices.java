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
import com.epam.ta.reportportal.database.entity.user.User;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Token services for GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class GitHubTokenServices implements ResourceServerTokenServices {

	private final GitHubUserReplicator replicator;

	public GitHubTokenServices(GitHubUserReplicator replicatingPrincipalExtractor) {
		this.replicator = replicatingPrincipalExtractor;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
		User user = replicator.replicateUser(accessToken);

		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getId(), "N/A",
				AuthUtils.AS_AUTHORITIES.apply(user.getRole()));

		Map<String, Serializable> extensionProperties = Collections.singletonMap("upstream_token", accessToken);
		OAuth2Request request = new OAuth2Request(null, null, null, true, null, null, null, null, extensionProperties);
		return new OAuth2Authentication(request, token);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

}
