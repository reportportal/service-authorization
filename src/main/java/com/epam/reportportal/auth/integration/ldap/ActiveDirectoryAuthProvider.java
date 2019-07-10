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
