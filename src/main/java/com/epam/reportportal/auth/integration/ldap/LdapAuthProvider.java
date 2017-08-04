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
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;
import com.epam.reportportal.auth.store.entity.ldap.PasswordEncoderType;
import com.epam.ta.reportportal.commons.accessible.Accessible;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.encoding.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

/**
 * Plain LDAP auth provider
 *
 * @author Andrei Varabyeu
 */
public class LdapAuthProvider extends EnableableAuthProvider {

	private final LdapUserReplicator ldapUserReplicator;

	public LdapAuthProvider(AuthConfigRepository authConfigRepository, LdapUserReplicator ldapUserReplicator) {
		super(authConfigRepository);
		this.ldapUserReplicator = ldapUserReplicator;
	}

	@Override
	protected boolean isEnabled() {
		return authConfigRepository.findLdap(true).isPresent();
	}

	@Override
	protected AuthenticationProvider getDelegate() {
		LdapConfig ldap = authConfigRepository.findLdap(true).orElseThrow(() -> new BadCredentialsException("LDAP is not configured"));

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(singletonList(ldap.getServer()),
				ldap.getBaseDn());
		contextSource.setPassword(ldap.getManagerPassword());
		contextSource.setUserDn(ldap.getManagerDn());
		contextSource.afterPropertiesSet();

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> builder = new LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>()
				.authoritiesMapper(authorities -> AuthUtils.AS_AUTHORITIES.apply(UserRole.USER))
				.userDetailsContextMapper(new DetailsContextMapper(ldapUserReplicator, ldap.getSynchronizationAttributes()))
				.groupSearchBase(ldap.getGroupSearchBase()).groupSearchFilter(ldap.getGroupSearchFilter())
				.userSearchFilter(ldap.getUserSearchFilter()).contextSource(contextSource);

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>.PasswordCompareConfigurer passwordCompareConfigurer = builder
				.passwordCompare();

		if (!isNullOrEmpty(ldap.getPasswordAttribute())) {
			passwordCompareConfigurer.passwordAttribute(ldap.getPasswordAttribute());

		}

		ofNullable(ldap.getPasswordEncoderType()).ifPresent(encoder -> builder.passwordEncoder(ENCODER_MAPPING.get(encoder)));

		if (!isNullOrEmpty(ldap.getUserDnPattern())) {
			builder.userDnPatterns(ldap.getUserDnPattern());
		}

		try {
			return (AuthenticationProvider) Accessible.on(builder)
					.method(LdapAuthenticationProviderConfigurer.class.getDeclaredMethod("build")).invoke();
		} catch (Throwable e) {
			throw new ReportPortalException("Cannot build LDAP auth provider", e);
		}
	}

	//@formatter:off
	@VisibleForTesting
	static final Map<PasswordEncoderType, PasswordEncoder> ENCODER_MAPPING = ImmutableMap.<PasswordEncoderType, PasswordEncoder>builder()
			.put(PasswordEncoderType.LDAP_SHA, new LdapShaPasswordEncoder())
			.put(PasswordEncoderType.MD4, new Md4PasswordEncoder())
			.put(PasswordEncoderType.MD5, new Md5PasswordEncoder())
			.put(PasswordEncoderType.SHA, new ShaPasswordEncoder())
			.put(PasswordEncoderType.PLAIN, new PlaintextPasswordEncoder())
			.build();
	//@formatter:on

}
