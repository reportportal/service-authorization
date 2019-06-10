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

import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Objects;

import static com.epam.reportportal.auth.integration.github.ExternalOauth2TokenConverter.UPSTREAM_TOKEN;

/**
 * GitHUB synchronization endpoint
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@RestController
public class GithubEndpoint {

	private final GitHubUserReplicator replicator;

	@Autowired
	public GithubEndpoint(GitHubUserReplicator replicator) {
		this.replicator = replicator;
	}

	@ApiOperation(value = "Synchronizes logged-in GitHub user")
	@RequestMapping(value = { "/sso/me/github/synchronize" }, method = RequestMethod.POST)
	public OperationCompletionRS synchronize(OAuth2Authentication user) {
		Serializable upstreamToken = user.getOAuth2Request().getExtensions().get(UPSTREAM_TOKEN);
		BusinessRule.expect(upstreamToken, Objects::nonNull)
				.verify(ErrorType.INCORRECT_AUTHENTICATION_TYPE, "Cannot synchronize GitHub User");
		this.replicator.synchronizeUser(upstreamToken.toString());
		return new OperationCompletionRS("User info successfully synchronized");
	}
}
