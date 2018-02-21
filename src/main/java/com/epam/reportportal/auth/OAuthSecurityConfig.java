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

/**
 * Main Security Extension Point.
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
//@Configuration
//@EnableOAuth2Client
//@Order(6)
//@Conditional(OAuthSecurityConfig.HasExtensionsCondition.class)
//public class OAuthSecurityConfig extends WebSecurityConfigurerAdapter {
public class OAuthSecurityConfig {

//	public static final String SSO_LOGIN_PATH = "/sso/login";
//
//	@Autowired
//	private OAuth2ClientContext oauth2ClientContext;
//
//	@Autowired
//	protected OAuthSuccessHandler authSuccessHandler;
//
//	//    @Autowired
//	//    private List<OAuthProvider> authProviders;
//
//	/**
//	 * Extension point. Other Implementations can add their own OAuth processing filters
//	 *
//	 * @param oauth2ClientContext OAuth Client context
//	 * @return List of additional OAuth processing filters
//	 * @throws Exception in case of error
//	 */
//	protected List<OAuth2ClientAuthenticationProcessingFilter> getAdditionalFilters(OAuth2ClientContext oauth2ClientContext)
//			throws Exception {
//		return Collections.emptyList();
//	}
//
//	@Override
//	protected final void configure(HttpSecurity http) throws Exception {
//		//@formatter:off
//        http
//                .antMatcher("/**")
//                .authorizeRequests()
//                .antMatchers(SSO_LOGIN_PATH + "/**", "/webjars/**", "/index.html", "/epam/**", "/info", "/health", "/api-docs/**")
//                .permitAll()
//                .anyRequest()
//                .authenticated()
//                .and().csrf().disable()
//                .sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//        CompositeFilter authCompositeFilter = new CompositeFilter();
//        List<OAuth2ClientAuthenticationProcessingFilter> additionalFilters = ImmutableList.<OAuth2ClientAuthenticationProcessingFilter>builder()
//                .addAll(getDefaultFilters(oauth2ClientContext))
//                .addAll(getAdditionalFilters(oauth2ClientContext)).build();
//
//		/* make sure filters have correct exception handler */
//        additionalFilters.forEach(filter -> filter.setAuthenticationFailureHandler(OAUTH_ERROR_HANDLER));
//        authCompositeFilter.setFilters(additionalFilters);
//
//        //install additional OAuth Authentication filters
//        http.addFilterAfter(authCompositeFilter, BasicAuthenticationFilter.class);
//        //@formatter:on
//	}
//
//	@Bean
//		//	Map<String, OAuthProvider> oauthProviders(List<OAuthProvider> providers) {
//	Map<String, OAuthProvider> oauthProviders() {
//		return Collections.emptyMap();
//	}
//
//	@Bean
//	FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
//		FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<>();
//		registration.setFilter(filter);
//		registration.setOrder(-100);
//		return registration;
//	}
//
//	private List<OAuth2ClientAuthenticationProcessingFilter> getDefaultFilters(OAuth2ClientContext oauth2ClientContext) {
//		//        return authProviders.stream().map(provider -> {
//		//            OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
//		//                    provider.buildPath(SSO_LOGIN_PATH));
//		//            filter.setRestTemplate(provider.getOAuthRestOperations(oauth2ClientContext));
//		//            filter.setTokenServices(provider.getTokenServices());
//		//            filter.setAuthenticationSuccessHandler(authSuccessHandler);
//		//            return filter;
//		//        }).collect(Collectors.toList());
//		return Collections.emptyList();
//	}
//
//	//
//	//	/**
//	//	 * Condition. Load this config is there are no subclasses in the application context
//	//	 */
//	//	protected static class HasExtensionsCondition extends SpringBootCondition {
//	//
//	//		@Override
//	//		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
//	//			String[] enablers = context.getBeanFactory().getBeanNamesForAnnotation(EnableOAuth2Client.class);
//	//			boolean extensions = Arrays.stream(enablers)
//	//					.filter(name -> !context.getBeanFactory().getType(name).equals(OAuthSecurityConfig.class))
//	//					.anyMatch(name -> context.getBeanFactory().isTypeMatch(name, OAuthSecurityConfig.class));
//	//			if (extensions) {
//	//				return ConditionOutcome.noMatch("found @EnableOAuth2Client on a OAuthSecurityConfig subclass");
//	//			} else {
//	//				return ConditionOutcome.match("found no @EnableOAuth2Client on a OAuthSecurityConfig subsclass");
//	//			}
//	//
//	//		}
//	//	}
//	//
//	private static final AuthenticationFailureHandler OAUTH_ERROR_HANDLER = (request, response, exception) -> {
//		response.sendRedirect(UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
//				.replacePath("ui/#login")
//				.replaceQuery("errorAuth=" + exception.getMessage())
//				.build()
//				.toUriString());
//	};

}
