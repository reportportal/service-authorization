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

package com.epam.reportportal.auth.config;

import com.epam.reportportal.auth.OAuthSuccessHandler;
import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.basic.BasicPasswordAuthenticationProvider;
import com.epam.reportportal.auth.basic.DatabaseUserDetailsService;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.github.ExternalOauth2TokenConverter;
import com.epam.reportportal.auth.integration.ldap.ActiveDirectoryAuthProvider;
import com.epam.reportportal.auth.integration.ldap.DetailsContextMapper;
import com.epam.reportportal.auth.integration.ldap.LdapAuthProvider;
import com.epam.reportportal.auth.integration.ldap.LdapUserReplicator;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.oauth.AccessTokenStore;
import com.epam.reportportal.auth.oauth.OAuthProvider;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.entity.ServerSettings;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.*;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.Filter;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfiguration {

	@EnableOAuth2Client
	@Configuration
	@Conditional(GlobalWebSecurityConfig.HasExtensionsCondition.class)
	@Order(5)
	public static class GlobalWebSecurityConfig extends WebSecurityConfigurerAdapter {

		public static final String SSO_LOGIN_PATH = "/sso/login";

		private OAuth2ClientContext oauth2ClientContext;
		private OAuthSuccessHandler successHandler;
		private AuthenticationFailureHandler authenticationFailureHandler;
		private IntegrationRepository authConfigRepository;
		private LdapUserReplicator ldapUserReplicator;
		private List<OAuthProvider> authProviders;

		@Autowired
		public GlobalWebSecurityConfig(OAuth2ClientContext oauth2ClientContext, AuthenticationFailureHandler authenticationFailureHandler,
				IntegrationRepository authConfigRepository, LdapUserReplicator ldapUserReplicator, List<OAuthProvider> authProviders) {
			this.oauth2ClientContext = oauth2ClientContext;
			this.authenticationFailureHandler = authenticationFailureHandler;
			this.authConfigRepository = authConfigRepository;
			this.ldapUserReplicator = ldapUserReplicator;
			this.authProviders = authProviders;
		}

		@Autowired
		public void setSuccessHandler(OAuthSuccessHandler successHandler) {
			this.successHandler = successHandler;
		}

		private List<OAuth2ClientAuthenticationProcessingFilter> getDefaultFilters(OAuth2ClientContext oauth2ClientContext) {
			return authProviders.stream().map(provider -> {
				OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(provider.buildPath(
						SSO_LOGIN_PATH));
				filter.setRestTemplate(provider.getOAuthRestOperations(oauth2ClientContext));
				filter.setTokenServices(provider.getTokenServices());
				filter.setAuthenticationSuccessHandler(successHandler);
				return filter;
			}).collect(Collectors.toList());
		}

		protected List<OAuth2ClientAuthenticationProcessingFilter> getAdditionalFilters(OAuth2ClientContext oauth2ClientContext) {
			return Collections.emptyList();
		}

		/**
		 * Condition. Load this config is there are no subclasses in the application context
		 */
		protected static class HasExtensionsCondition extends SpringBootCondition {

			@Override
			public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
				String[] enablers = context.getBeanFactory().getBeanNamesForAnnotation(EnableOAuth2Client.class);
				boolean extensions = Arrays.stream(enablers)
						.filter(name -> !context.getBeanFactory().getType(name).equals(GlobalWebSecurityConfig.class))
						.anyMatch(name -> context.getBeanFactory().isTypeMatch(name, GlobalWebSecurityConfig.class));
				if (extensions) {
					return ConditionOutcome.noMatch("found @EnableOAuth2Client on a OAuthSecurityConfig subclass");
				} else {
					return ConditionOutcome.match("found no @EnableOAuth2Client on a OAuthSecurityConfig subsclass");
				}

			}
		}

		@Bean
		public Map<String, OAuthProvider> oauthProviders(List<OAuthProvider> providers) {
			return providers.stream().collect(Collectors.toMap(OAuthProvider::getName, p -> p));
		}

		@Bean
		public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
			FilterRegistrationBean registration = new FilterRegistrationBean();
			registration.setFilter(filter);
			registration.setOrder(-100);
			return registration;
		}

		@Bean("activeDirectoryDetailsContextMapper")
		public DetailsContextMapper activeDirectoryDetailsContextMapper() {
			return new DetailsContextMapper(
					ldapUserReplicator,
					() -> ParameterUtils.getLdapSyncAttributes(authConfigRepository.findAllByTypeIn(AuthIntegrationType.ACTIVE_DIRECTORY.getName()).stream().findFirst()
							.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND)))

			);
		}

		@Bean("ldapDetailsContextMapper")
		public DetailsContextMapper ldapDetailsContextMapper() {
			return new DetailsContextMapper(
					ldapUserReplicator,
					() -> ParameterUtils.getLdapSyncAttributes(authConfigRepository.findAllByTypeIn(AuthIntegrationType.LDAP.getName()).stream().findFirst()
							.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND))

					)
			);
		}

		@Bean
		public Filter forwardedHeaderFilter() {
			return new ForwardedHeaderFilter();
		}

		@Bean
		public FilterRegistrationBean<Filter> forwardedHeaderFilterRegistrationBean() {
			FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
			filterRegistrationBean.setFilter(forwardedHeaderFilter());
			filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return filterRegistrationBean;
		}

		@Override
		protected final void configure(HttpSecurity http) throws Exception {
			CompositeFilter authCompositeFilter = new CompositeFilter();
			List<OAuth2ClientAuthenticationProcessingFilter> additionalFilters = ImmutableList.<OAuth2ClientAuthenticationProcessingFilter>builder()
					.addAll(getDefaultFilters(oauth2ClientContext))
					.addAll(getAdditionalFilters(oauth2ClientContext))
					.build();

			/* make sure filters have correct exception handler */
			additionalFilters.forEach(filter -> filter.setAuthenticationFailureHandler(authenticationFailureHandler));
			authCompositeFilter.setFilters(additionalFilters);

			//@formatter:off
        	http
                .antMatcher("/**")
                .authorizeRequests()
                .antMatchers(SSO_LOGIN_PATH + "/**", "/epam/**", "/info", "/health", "/api-docs/**", "/saml/**", "/templates/**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
					.csrf().disable()
				.formLogin().disable()
				.sessionManagement()
                	.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.addFilterAfter(authCompositeFilter, BasicAuthenticationFilter.class);
       		 //@formatter:on
		}

		@Autowired
		private ApplicationEventPublisher eventPublisher;

		@Autowired
		@Qualifier("activeDirectoryDetailsContextMapper")
		DetailsContextMapper activeDirectoryContextMapper;

		@Autowired
		@Qualifier("ldapDetailsContextMapper")
		DetailsContextMapper ldapContextMapper;

		@Bean
		public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
			return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) {
			auth.authenticationProvider(basicPasswordAuthProvider())
					.authenticationEventPublisher(authenticationEventPublisher(eventPublisher))
					.authenticationProvider(activeDirectoryAuthProvider())
					.authenticationProvider(ldapAuthProvider());
		}

		@Bean
		public AuthenticationProvider activeDirectoryAuthProvider() {
			return new ActiveDirectoryAuthProvider(authConfigRepository, eventPublisher, activeDirectoryContextMapper);
		}

		@Bean
		public AuthenticationProvider ldapAuthProvider() {
			return new LdapAuthProvider(authConfigRepository, eventPublisher, ldapContextMapper);
		}

		@Bean
		protected UserDetailsService userDetailsService() {
			return new DatabaseUserDetailsService();
		}

		@Bean
		public AuthenticationProvider basicPasswordAuthProvider() {
			BasicPasswordAuthenticationProvider provider = new BasicPasswordAuthenticationProvider();
			provider.setUserDetailsService(userDetailsService());
			provider.setPasswordEncoder(passwordEncoder());
			return provider;
		}

		public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Override
		@Primary
		@Bean
		public AuthenticationManager authenticationManager() throws Exception {
			return super.authenticationManager();
		}
	}

	@Configuration
	@EnableAuthorizationServer
	public static class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

		private static final String SECRET_KEY = "secret.key";

		@Value("${rp.jwt.signing-key}")
		private String signingKey;

		@Value("${rp.jwt.token.validity-period}")
		private Integer tokenValidity;

		private final AuthenticationManager authenticationManager;

		@Autowired
		private DatabaseUserDetailsService userDetailsService;

		@Autowired
		private ServerSettingsRepository serverSettingsRepository;

		@Autowired
		public AuthorizationServerConfiguration(AuthenticationManager authenticationManager) {
			this.authenticationManager = authenticationManager;
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
			//@formatter:off

			endpoints
					.pathMapping("/oauth/token", "/sso/oauth/token")
					.pathMapping("/oauth/token_key", "/sso/oauth/token_key")
					.pathMapping("/oauth/check_token", "/sso/oauth/check_token")
					.pathMapping("/oauth/authorize", "/sso/oauth/authorize")
					.pathMapping("/oauth/confirm_access", "/sso/oauth/confirm_access")
					.tokenStore(jwtTokenStore())
//					.exceptionTranslator(new OAuthErrorHandler(new ReportPortalExceptionResolver(new DefaultErrorResolver(ExceptionMappings.DEFAULT_MAPPING))))
					.accessTokenConverter(accessTokenConverter(externalOauth2TokenConverter(defaultUserAuthenticationConverter())))
					.authenticationManager(authenticationManager);
			//@formatter:on
		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			//@formatter:off
            clients.inMemory()
                    .withClient(ReportPortalClient.ui.name())
                    	.secret("{bcrypt}$2a$10$ka8W./nA2Uiqsd2uOzazdu2lMbipaMB6RJNInB1Y0NMKQzj7plsie")
                    	.authorizedGrantTypes("refresh_token", "password")
                    	.scopes("ui")
						.accessTokenValiditySeconds(tokenValidity)
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

		@Override
		public void configure(AuthorizationServerSecurityConfigurer security) {

			security.tokenKeyAccess("hasAuthority('ROLE_INTERNAL')").checkTokenAccess("hasAuthority('ROLE_INTERNAL')");

		}

		@Bean(value = "jwtTokenStore")
		@Primary
		public TokenStore jwtTokenStore() {
			AccessTokenConverter accessTokenConverter = externalOauth2TokenConverter(defaultUserAuthenticationConverter());
			JwtAccessTokenConverter jwtTokenEnhancer = accessTokenConverter(accessTokenConverter);
			return new JwtTokenStore(jwtTokenEnhancer);
		}

		@Bean
		public UserAuthenticationConverter defaultUserAuthenticationConverter() {
			DefaultUserAuthenticationConverter defaultUserAuthenticationConverter = new DefaultUserAuthenticationConverter();
			defaultUserAuthenticationConverter.setUserDetailsService(userDetailsService);
			return defaultUserAuthenticationConverter;
		}

		@Bean
		public AccessTokenConverter externalOauth2TokenConverter(UserAuthenticationConverter userAuthenticationConverter) {
			ExternalOauth2TokenConverter accessTokenConverter = new ExternalOauth2TokenConverter();
			accessTokenConverter.setUserTokenConverter(userAuthenticationConverter);
			return accessTokenConverter;
		}

		@Bean
		public JwtAccessTokenConverter accessTokenConverter(AccessTokenConverter accessTokenConverter) {
			JwtAccessTokenConverter jwtConverter = new JwtAccessTokenConverter();
			jwtConverter.setSigningKey(getSecret());
			jwtConverter.setAccessTokenConverter(accessTokenConverter);
			return jwtConverter;
		}

		private String getSecret() {
			if (!StringUtils.isEmpty(signingKey)) {
				return signingKey;
			}
			Optional<ServerSettings> secretKey = serverSettingsRepository.findByKey(SECRET_KEY);
			return secretKey.isPresent() ? secretKey.get().getValue() : serverSettingsRepository.generateSecret();
		}

		@Bean
		@Primary
		public DefaultTokenServices tokenServices() {
			DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
			defaultTokenServices.setTokenStore(jwtTokenStore());
			defaultTokenServices.setSupportRefreshToken(true);
			defaultTokenServices.setAuthenticationManager(authenticationManager);
			AccessTokenConverter accessTokenConverter = externalOauth2TokenConverter(defaultUserAuthenticationConverter());
			JwtAccessTokenConverter accessTokenEnhancer = accessTokenConverter(accessTokenConverter);
			defaultTokenServices.setTokenEnhancer(accessTokenEnhancer);
			return defaultTokenServices;
		}

		@Bean(value = "databaseTokenServices")
		public DefaultTokenServices databaseTokenServices(@Autowired AccessTokenStore accessTokenStore,
				@Autowired ClientDetailsService clientDetailsService) {
			DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
			defaultTokenServices.setTokenStore(accessTokenStore);
			defaultTokenServices.setClientDetailsService(clientDetailsService);
			defaultTokenServices.setSupportRefreshToken(false);
			defaultTokenServices.setAuthenticationManager(authenticationManager);
			return defaultTokenServices;
		}

	}

	@Configuration
	@EnableResourceServer
	public static class ResourceServerAuthConfiguration extends ResourceServerConfigurerAdapter {
		@Override
		public void configure(HttpSecurity http) throws Exception {
			http.requestMatchers()
					.antMatchers("/sso/me/**", "/sso/internal/**", "/settings/**")
					.and()
					.authorizeRequests()
					.antMatchers("/settings/**")
					.hasRole("ADMINISTRATOR")
					.antMatchers("/sso/internal/**")
					.hasRole("INTERNAL")
					.anyRequest()
					.authenticated()
					.and()
					.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		}

	}
}
