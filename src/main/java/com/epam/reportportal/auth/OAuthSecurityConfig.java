/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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
/*
 * This file is part of Report Portal.
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CompositeFilter;

import java.util.Collections;
import java.util.List;

/**
 * Main Security Extension Point.
 *
 * @author Andrei Varabyeu
 */
@Configuration
@EnableOAuth2Client
@Order(6)
@ConditionalOnMissingBean(OAuthSecurityConfig.class)
public class OAuthSecurityConfig extends WebSecurityConfigurerAdapter {

	private static final String SSO_LOGIN_PATH = "/sso/login";

	@Autowired
	private OAuth2ClientContext oauth2ClientContext;

	protected List<OAuth2ClientAuthenticationProcessingFilter> getAdditionalFilters(OAuth2ClientContext oauth2ClientContext) throws Exception{
		return Collections.emptyList();
	}

	@Override
	protected final void configure(HttpSecurity http) throws Exception {
		//@formatter:off
			 http
				.antMatcher("/**")
					 .authorizeRequests()
				.antMatchers(SSO_LOGIN_PATH + "/**", "/webjars/**", "/index.html", "/epam/**", "/info", "/health")
					 .permitAll()
				.anyRequest()
					 .authenticated()
				.and().csrf().disable();

		CompositeFilter authCompositeFilter = new CompositeFilter();
		authCompositeFilter.setFilters(ImmutableList.<OAuth2ClientAuthenticationProcessingFilter>builder()
						.addAll(getDefaultFilters(oauth2ClientContext))
						.addAll(getAdditionalFilters(oauth2ClientContext)).build());

		//install additional OAuth Authentication filters
		 http.addFilterAfter(authCompositeFilter, BasicAuthenticationFilter.class);
		//@formatter:on
	}

	@Bean
	FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(filter);
		registration.setOrder(-100);
		return registration;
	}

	@Bean
	@ConfigurationProperties("github")
	ClientResources github() {
		return new ClientResources();
	}

	private List<OAuth2ClientAuthenticationProcessingFilter> getDefaultFilters(OAuth2ClientContext oauth2ClientContext) {
		OAuth2ClientAuthenticationProcessingFilter githubFilter = new OAuth2ClientAuthenticationProcessingFilter(
				SSO_LOGIN_PATH + "/github");
		OAuth2RestTemplate githubTemplate = new OAuth2RestTemplate(github().getClient(), oauth2ClientContext);
		githubFilter.setRestTemplate(githubTemplate);
		githubFilter
				.setTokenServices(new UserInfoTokenServices(github().getResource().getUserInfoUri(), github().getResource().getClientId()));
		return Collections.singletonList(githubFilter);
	}

}