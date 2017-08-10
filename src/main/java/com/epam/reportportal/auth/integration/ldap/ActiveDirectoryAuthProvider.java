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

import com.epam.reportportal.auth.AuthUtils;
import com.epam.reportportal.auth.EnableableAuthProvider;
import com.epam.reportportal.auth.store.AuthConfigRepository;
import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import static java.util.Optional.ofNullable;

/**
 * Active Directory provider
 * @author Andrei Varabyeu
 */
public class ActiveDirectoryAuthProvider extends EnableableAuthProvider {

	private final LdapUserReplicator ldapUserReplicator;

	public ActiveDirectoryAuthProvider(AuthConfigRepository authConfigRepository, LdapUserReplicator ldapUserReplicator) {
		super(authConfigRepository);
		this.ldapUserReplicator = ldapUserReplicator;
	}

	@Override
	protected boolean isEnabled() {
		return authConfigRepository.findActiveDirectory(true).isPresent();
	}

	@Override
	protected AuthenticationProvider getDelegate() {

		ActiveDirectoryConfig adConfig = authConfigRepository.findActiveDirectory(true)
				.orElseThrow(() -> new BadCredentialsException("LDAP is not configured"));

		ActiveDirectoryLdapAuthenticationProvider adAuth = new ActiveDirectoryLdapAuthenticationProvider(adConfig.getDomain(),
				adConfig.getUrl(), adConfig.getBaseDn());

		adAuth.setAuthoritiesMapper(authorities -> AuthUtils.AS_AUTHORITIES.apply(UserRole.USER));
		adAuth.setUserDetailsContextMapper(new DetailsContextMapper(ldapUserReplicator, adConfig.getSynchronizationAttributes()));
		return adAuth;
	}

	//	public class CustomLdapProvider implements AuthenticationProvider {
	//
	//		private final ActiveDirectoryLdapAuthenticationProvider delegate;
	//
	//		public CustomLdapProvider(ActiveDirectoryLdapAuthenticationProvider delegate) {
	//			this.delegate = delegate;
	//		}
	//
	//		@Override
	//		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	//			Authentication auth = this.delegate.authenticate(authentication);
	//			if (null == auth) {
	//				return null;
	//			}
	//			return new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(),
	//					AuthUtils.AS_AUTHORITIES.apply(UserRole.USER));
	//		}
	//
	//		@Override
	//		public boolean supports(C    lass<?> authentication) {
	//			return this.delegate.supports(authentication);
	//		}
	//	}
}
