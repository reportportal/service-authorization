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

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

import static org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityDsl.serviceProvider;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Configuration
@Order(4)
public class SamlSecurityConfiguration extends SamlServiceProviderSecurityConfiguration {

	private SamlServerConfiguration serverConfiguration;

	public SamlSecurityConfiguration(SamlServiceProviderServerBeanConfiguration configuration, SamlServerConfiguration spConfiguration) {
		super(configuration);
		this.serverConfiguration = spConfiguration;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);
		http.apply(serviceProvider()).configure(serverConfiguration);
	}
}
