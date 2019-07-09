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
import com.epam.ta.reportportal.commons.accessible.Accessible;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.exception.ReportPortalException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;

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

	public LdapAuthProvider(IntegrationRepository authConfigRepository, LdapUserReplicator ldapUserReplicator) {
		super(authConfigRepository);
		this.ldapUserReplicator = ldapUserReplicator;
	}

	@Override
	protected boolean isEnabled() {
		return integrationRepository.findLdap(true).isPresent();
	}

	@Override
	protected AuthenticationProvider getDelegate() {
		LdapConfig ldap = integrationRepository.findLdap(true).orElseThrow(() -> new BadCredentialsException(
				"LDAP is not configured"));

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(singletonList(ldap.getUrl()),
				ldap.getBaseDn()
		);
		ofNullable(ldap.getManagerPassword()).ifPresent(contextSource::setPassword);
		ofNullable(ldap.getManagerDn()).ifPresent(contextSource::setUserDn);
		contextSource.afterPropertiesSet();

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> builder = new LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>()
				.contextSource(contextSource)
				.ldapAuthoritiesPopulator(new NullLdapAuthoritiesPopulator())
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
			final PasswordEncoder delegate = PasswordEncoderFactories.createDelegatingPasswordEncoder();
			builder.passwordEncoder(new org.springframework.security.crypto.password.PasswordEncoder() {

				@Override
				public String encode(CharSequence rawPassword) {
					return delegate.encode(rawPassword);
				}

				@Override
				public boolean matches(CharSequence rawPassword, String encodedPassword) {
					return delegate.matches(rawPassword, encodedPassword);
				}
			});

		});

		if (!isNullOrEmpty(ldap.getUserDnPattern())) {
			builder.userDnPatterns(ldap.getUserDnPattern());
		}

		try {
			return (AuthenticationProvider) Accessible.on(builder)
					.method(LdapAuthenticationProviderConfigurer.class.getDeclaredMethod("build"))
					.invoke();
		} catch (Throwable e) {
			throw new ReportPortalException("Cannot build LDAP auth provider", e);
		}
	}

}
