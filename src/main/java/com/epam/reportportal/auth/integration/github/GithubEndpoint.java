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
