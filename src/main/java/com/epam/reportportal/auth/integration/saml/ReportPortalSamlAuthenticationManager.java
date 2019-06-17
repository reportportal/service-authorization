/*
 * Copyright 2019 EPAM Systems
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
package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.AuthUtils;
import com.epam.ta.reportportal.database.entity.user.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;

/**
 * Implementation of authentication manager for SAML integration
 *
 * @author Yevgeniy Svalukhin
 */
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
