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
package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.util.AuthUtils;
import com.epam.ta.reportportal.entity.user.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;
import org.springframework.stereotype.Component;

/**
 * Implementation of authentication manager for SAML integration
 *
 * @author Yevgeniy Svalukhin
 */
@Component
public class ReportPortalSamlAuthenticationManager implements AuthenticationManager {

	private SamlUserReplicator samlUserReplicator;

	public ReportPortalSamlAuthenticationManager(SamlUserReplicator samlUserReplicator) {
		this.samlUserReplicator = samlUserReplicator;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (authentication instanceof DefaultSamlAuthentication) {
			ReportPortalSamlAuthentication reportPortalSamlAuthentication = new ReportPortalSamlAuthentication((DefaultSamlAuthentication) authentication);
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
