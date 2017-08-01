package com.epam.reportportal.auth.ldap;

import com.epam.reportportal.auth.EnableableAuthProvider;
import com.epam.ta.reportportal.commons.accessible.Accessible;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.settings.LdapConfig;
import com.epam.ta.reportportal.database.entity.settings.PasswordEncoderType;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.encoding.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import java.util.Arrays;
import java.util.Map;

import static java.util.Collections.singletonList;

public class LdapAuthProvider extends EnableableAuthProvider {

	private final ServerSettingsRepository serverSettingsRepository;

	public LdapAuthProvider(ServerSettingsRepository serverSettingsRepository) {
		this.serverSettingsRepository = serverSettingsRepository;

	}

	@Override
	protected boolean isEnabled() {
		return false;
	}

	@Override
	protected AuthenticationProvider getDelegate() {
		ServerSettings settings = serverSettingsRepository.findOne("default");
		LdapConfig ldap = settings.getAuthConfig().getLdap();

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> builder = new LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>()
				.groupSearchBase(ldap.getGroupSearchBase()).userDnPatterns(ldap.getUserDnPattern())
				.contextSource(new DefaultSpringSecurityContextSource(singletonList(ldap.getServer()), ldap.getBaseDn())).passwordCompare()
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
