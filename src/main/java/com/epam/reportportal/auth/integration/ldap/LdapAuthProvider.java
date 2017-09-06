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
import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder;
import org.springframework.security.authentication.encoding.Md4PasswordEncoder;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
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

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(singletonList(ldap.getUrl()),
				ldap.getBaseDn());
		ofNullable(ldap.getManagerPassword()).ifPresent(contextSource::setPassword);
		ofNullable(ldap.getManagerDn()).ifPresent(contextSource::setUserDn);
		contextSource.afterPropertiesSet();

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> builder = new LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>()
				.contextSource(contextSource)
				.authoritiesMapper(authorities -> AuthUtils.AS_AUTHORITIES.apply(UserRole.USER))
				.ldapAuthoritiesPopulator((userData, username) -> AuthUtils.AS_AUTHORITIES.apply(UserRole.USER))
				.userDetailsContextMapper(new DetailsContextMapper(ldapUserReplicator, ldap.getSynchronizationAttributes()));

		/*
		 * Basically, groups are not used
		 */
		ofNullable(ldap.getGroupSearchFilter()).ifPresent(builder::groupSearchFilter);
		ofNullable(ldap.getGroupSearchBase()).ifPresent(builder::groupSearchBase);
		ofNullable(ldap.getUserSearchFilter()).ifPresent(builder::userSearchFilter);



		ofNullable(ldap.getPasswordEncoderType()).ifPresent(it -> {
			LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>.PasswordCompareConfigurer passwordCompareConfigurer = builder
					.passwordCompare();
			if (!isNullOrEmpty(ldap.getPasswordAttribute())) {
				passwordCompareConfigurer.passwordAttribute(ldap.getPasswordAttribute());
			}

			/*
			 * DIRTY HACK. If LDAP's password has solt, ldaptemplate.compare operation does not work
			 * since we don't know server's salt.
			 * To enable local password comparison, we need to provide password encoder from crypto's package
			 * This is why we just wrap old encoder with new one interface
			 * New encoder cannot be used everywhere since it does not have implementation for LDAP
			 */
			final PasswordEncoder delegate = ENCODER_MAPPING.get(ldap.getPasswordEncoderType());
			builder.passwordEncoder(new org.springframework.security.crypto.password.PasswordEncoder() {

				@Override
				public String encode(CharSequence rawPassword) {
					return delegate.encodePassword(rawPassword.toString(), null);
				}

				@Override
				public boolean matches(CharSequence rawPassword, String encodedPassword) {
					return delegate.isPasswordValid(encodedPassword, rawPassword.toString(), null);
				}
			});

		});

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
