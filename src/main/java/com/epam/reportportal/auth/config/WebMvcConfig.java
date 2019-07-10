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

import com.epam.ta.reportportal.commons.ExceptionMappings;
import com.epam.ta.reportportal.commons.exception.message.DefaultExceptionMessageBuilder;
import com.epam.ta.reportportal.commons.exception.rest.DefaultErrorResolver;
import com.epam.ta.reportportal.commons.exception.rest.ReportPortalExceptionResolver;
import com.epam.ta.reportportal.commons.exception.rest.RestErrorDefinition;
import com.epam.ta.reportportal.commons.exception.rest.RestExceptionHandler;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/**
 * @author Pavel Bortnik
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Autowired
	private HttpMessageConverters messageConverters;

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}

	@Override
	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {

		RestErrorDefinition<Exception> authErrorDefinition = new RestErrorDefinition<>(HttpStatus.BAD_REQUEST,
				ErrorType.ACCESS_DENIED,
				new DefaultExceptionMessageBuilder()
		);

		Map<Class<? extends Throwable>, RestErrorDefinition> errorMappings = ImmutableMap.<Class<? extends Throwable>, RestErrorDefinition>builder()
				.put(OAuth2Exception.class, authErrorDefinition)
				.put(AuthenticationException.class, authErrorDefinition)
				.put(UsernameNotFoundException.class, authErrorDefinition)
				.putAll(ExceptionMappings.DEFAULT_MAPPING)
				.build();

		RestExceptionHandler handler = new RestExceptionHandler();
		handler.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		handler.setErrorResolver(new ReportPortalExceptionResolver(new DefaultErrorResolver(errorMappings)));
		handler.setMessageConverters(messageConverters.getConverters());
		exceptionResolvers.add(handler);
	}

}
