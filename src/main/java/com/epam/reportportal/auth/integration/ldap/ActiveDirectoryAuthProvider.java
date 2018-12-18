/*
 * Copyright 2017 EPAM Systems
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
package com.epam.reportportal.auth.integration.ldap;

import com.epam.reportportal.auth.EnableableAuthProvider;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.ldap.ActiveDirectoryConfig;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

/**
 * Active Directory provider
 *
 * @author Andrei Varabyeu
 */
public class ActiveDirectoryAuthProvider extends EnableableAuthProvider {

	private final LdapUserReplicator ldapUserReplicator;

	public ActiveDirectoryAuthProvider(IntegrationRepository authConfigRepository, LdapUserReplicator ldapUserReplicator) {
		super(authConfigRepository);
		this.ldapUserReplicator = ldapUserReplicator;
	}

	@Override
	protected boolean isEnabled() {
		return integrationRepository.findActiveDirectory(true).isPresent();
	}

	@Override
	protected AuthenticationProvider getDelegate() {

		ActiveDirectoryConfig adConfig = integrationRepository.findActiveDirectory(true).orElseThrow(() -> new BadCredentialsException(
				"Active Directory is not configured"));

		ActiveDirectoryLdapAuthenticationProvider adAuth = new ActiveDirectoryLdapAuthenticationProvider(adConfig.getDomain(),
				adConfig.getUrl(),
				adConfig.getBaseDn()
		);

		adAuth.setAuthoritiesMapper(new NullAuthoritiesMapper());
		adAuth.setUserDetailsContextMapper(new DetailsContextMapper(ldapUserReplicator, adConfig.getSynchronizationAttributes()));
		return adAuth;
	}
}
