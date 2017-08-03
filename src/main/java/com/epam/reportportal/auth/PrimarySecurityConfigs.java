/*
 * Copyright 2016 EPAM Systems
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
package com.epam.reportportal.auth;

import com.epam.reportportal.auth.integration.ldap.LdapAuthProvider;
import com.epam.reportportal.auth.store.OAuth2MongoTokenStore;
import com.epam.ta.reportportal.commons.ExceptionMappings;
import com.epam.ta.reportportal.commons.exception.rest.DefaultErrorResolver;
import com.epam.ta.reportportal.commons.exception.rest.ReportPortalExceptionResolver;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;

/**
 * Set of general Security configs. This class is not supposed to be extended
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */

class PrimarySecurityConfigs {

	@Configuration
	@EnableResourceServer
	protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
		@Override
		public void configure(HttpSecurity http) throws Exception {
			http.requestMatchers()
					.antMatchers("/sso/me/**", "/sso/internal/**", "/settings/**")
					.and()
					.authorizeRequests()
					.antMatchers("/settings/**").hasRole("ADMINISTRATOR")
					.antMatchers("/sso/internal/**").hasRole("INTERNAL")
					.anyRequest().authenticated()
					.and().sessionManagement()
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		}
	}

	@Configuration
	protected static class GlobalSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

		@Autowired
		private ServerSettingsRepository serverSettingsRepository;

		@Bean
		UserDetailsService userDetailsService() {
			return new DatabaseUserDetailsService();
		}

		@Bean
		AuthenticationProvider basicPasswordAuthProvider() {
			BasicPasswordAuthenticationProvider provider = new BasicPasswordAuthenticationProvider();
			provider.setUserDetailsService(userDetailsService());
			provider.setPasswordEncoder(new Md5PasswordEncoder());
			return provider;
		}

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(basicPasswordAuthProvider())
					.authenticationProvider(new LdapAuthProvider(serverSettingsRepository, ldapUserReplicator));
		}





	}

	@Configuration
	@EnableAuthorizationServer
	protected static class SsoOAuth2Config extends AuthorizationServerConfigurerAdapter {

		@Autowired
		private AuthenticationManager authenticationManager;

		@Autowired
		@Value("${rp.session.live}")
		private Integer sessionLive;

		@Bean
		public OAuth2MongoTokenStore tokenStore() {
			return new OAuth2MongoTokenStore();
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
			//@formatter:off
			endpoints
					.pathMapping("/oauth/token", "/sso/oauth/token")
					.pathMapping("/oauth/token_key", "/sso/oauth/token_key")
					.pathMapping("/oauth/check_token", "/sso/oauth/check_token")
					.pathMapping("/oauth/authorize", "/sso/oauth/authorize")
					.pathMapping("/oauth/confirm_access", "/sso/oauth/confirm_access")
					.authenticationManager(authenticationManager)
					.exceptionTranslator(new OAuthErrorHandler(new ReportPortalExceptionResolver(new DefaultErrorResolver(ExceptionMappings.DEFAULT_MAPPING))))

					.tokenStore(tokenStore());
			//@formatter:on
		}

		@Override
		public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
			security.tokenKeyAccess("hasAuthority('ROLE_INTERNAL')").checkTokenAccess("hasAuthority('ROLE_INTERNAL')");

		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			//@formatter:off
            clients.inMemory()
                    .withClient(ReportPortalClient.ui.name())
                    	.secret("uiman")
                    	.authorizedGrantTypes("refresh_token", "password")
                    	.scopes("ui")
						.accessTokenValiditySeconds(sessionLive)
                    .and()
                    .withClient(ReportPortalClient.api.name())
                    	.secret("apiman")
						.authorizedGrantTypes("password")
                    	.scopes("api")
                    	.accessTokenValiditySeconds(-1)

                    .and()
                    .withClient(ReportPortalClient.internal.name())
                    	.secret("internal_man")
                    	.authorizedGrantTypes("client_credentials").authorities("ROLE_INTERNAL")
                    	.scopes("internal");

            //@formatter:on
		}
	}
}
