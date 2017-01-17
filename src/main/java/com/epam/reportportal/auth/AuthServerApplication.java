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

import com.epam.reportportal.auth.store.entity.OAuth2AccessTokenEntity;
import com.epam.ta.reportportal.commons.ExceptionMappings;
import com.epam.ta.reportportal.commons.exception.rest.DefaultErrorResolver;
import com.epam.ta.reportportal.commons.exception.rest.ReportPortalExceptionResolver;
import com.epam.ta.reportportal.commons.exception.rest.RestExceptionHandler;
import com.epam.ta.reportportal.config.CacheConfiguration;
import com.epam.ta.reportportal.config.MongodbConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.session.data.mongo.AbstractMongoSessionConverter;
import org.springframework.session.data.mongo.JdkMongoSessionConverter;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * Application entry point
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@SpringBootApplication
@Import({ MongodbConfiguration.class, CacheConfiguration.class })
@EnableEurekaClient
@EnableMongoRepositories(basePackageClasses = OAuth2AccessTokenEntity.class)
public class AuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServerApplication.class, args);
	}

	/*
	 * Mongo HTTP session is used to share session between several instances
	 * Actually, authentication is stateless, but we need session storage to handle Authorization Flow
	 * of GitHub OAuth. This is alse the reason why there is requestContextListener - just to make
	 * request scope beans available for session commit during {@link org.springframework.session.web.http.SessionRepositoryFilter}
	 * execution
	 */
	@Configuration
	@EnableMongoHttpSession
	public static class MvcConfig extends WebMvcConfigurerAdapter {

		@Autowired
		private HttpMessageConverters messageConverters;

		@Bean
		public RequestContextListener requestContextListener(){
			return new RequestContextListener();
		}

		@Bean
		public AbstractMongoSessionConverter mongoSessionConverter(){
			return new JdkMongoSessionConverter();
		}

		@Override
		public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			RestExceptionHandler handler = new RestExceptionHandler();
			handler.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);

			DefaultErrorResolver defaultErrorResolver = new DefaultErrorResolver(ExceptionMappings.DEFAULT_MAPPING);
			handler.setErrorResolver(new ReportPortalExceptionResolver(defaultErrorResolver));
			handler.setMessageConverters(messageConverters.getConverters());
			exceptionResolvers.add(handler);
		}
	}

}