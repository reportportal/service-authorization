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
import com.epam.ta.reportportal.commons.accessible.Accessible;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.LdapConfig;
import com.epam.ta.reportportal.database.entity.settings.PasswordEncoderType;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.google.common.collect.ImmutableMap;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.encoding.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LdapAuthProvider extends EnableableAuthProvider {

	private final ServerSettingsRepository serverSettingsRepository;
	private final LdapUserReplicator ldapUserReplicator;

	public LdapAuthProvider(ServerSettingsRepository serverSettingsRepository, LdapUserReplicator ldapUserReplicator) {
		this.serverSettingsRepository = serverSettingsRepository;
		this.ldapUserReplicator = ldapUserReplicator;
	}

	@Override
	protected boolean isEnabled() {
		return ofNullable(serverSettingsRepository.findDefault())
				.flatMap(settings -> ofNullable(settings.getAuthConfig()))
				.flatMap(auth -> ofNullable(auth.getLdap()))
				.map(ldap -> isTrue(ldap.getEnabled()))
				.orElse(false);
	}

	@Override
	protected AuthenticationProvider getDelegate() {
		ServerSettings settings = serverSettingsRepository.findDefault();
		LdapConfig ldap = settings.getAuthConfig().getLdap();

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(singletonList(ldap.getServer()),
				ldap.getBaseDn());
		contextSource.setPassword(ldap.getManagerPassword());

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> builder = new LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>()
				.authoritiesMapper(authorities -> AuthUtils.AS_AUTHORITIES.apply(UserRole.USER))
				.userDetailsContextMapper(new LdapUserDetailsMapper() {
					@Override
					public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
							Collection<? extends GrantedAuthority> authorities) {
						UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
						String email = (String) ctx.getObjectAttribute(ldap.getEmailAttribute());
						ldapUserReplicator.replicateUser(userDetails.getUsername(), email);

						return userDetails;
					}
				})
				.groupSearchBase(ldap.getGroupSearchBase())
				.groupSearchFilter(ldap.getGroupSearchFilter())
				.userSearchFilter(ldap.getUserSearchFilter())
				.userDnPatterns(ldap.getUserDnPattern())
				.contextSource(contextSource).passwordCompare()
				.passwordAttribute(ldap.getPasswordAttribute()).and();
		builder
				.passwordEncoder(ENCODER_MAPPING.get(ldap.getPasswordEncoderType()));
		try {
			return  (AuthenticationProvider) Accessible.on(builder).method(LdapAuthenticationProviderConfigurer.class.getDeclaredMethod("build")).invoke();
		} catch (NoSuchMethodException e) {
			throw new ReportPortalException("Cannot build LDAP auth provider");
		}
	}

	private static final Map<PasswordEncoderType, PasswordEncoder> ENCODER_MAPPING = ImmutableMap
			.<PasswordEncoderType, PasswordEncoder>builder()
			.put(PasswordEncoderType.LDAP_SHA, new LdapShaPasswordEncoder())
			.put(PasswordEncoderType.MD4, new Md4PasswordEncoder())
			.put(PasswordEncoderType.MD5, new Md5PasswordEncoder())
			.put(PasswordEncoderType.SHA, new ShaPasswordEncoder())
			.put(PasswordEncoderType.PLAIN, new PlaintextPasswordEncoder())
			.build();

}
