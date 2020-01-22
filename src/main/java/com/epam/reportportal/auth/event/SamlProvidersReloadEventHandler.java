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
package com.epam.reportportal.auth.event;

import com.epam.reportportal.auth.integration.converter.SamlConverter;
import com.epam.ta.reportportal.entity.integration.Integration;
import org.springframework.context.ApplicationListener;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles SAML settings changes event and reload configuration of IDP in service provider configuration
 *
 * @author Yevgeniy Svalukhin
 */
@Component
public class SamlProvidersReloadEventHandler implements ApplicationListener<SamlProvidersReloadEvent> {

	private SamlServerConfiguration samlConfiguration;

	public SamlProvidersReloadEventHandler(SamlServerConfiguration spConfiguration) {
		this.samlConfiguration = spConfiguration;
	}

	@Override
	public void onApplicationEvent(SamlProvidersReloadEvent event) {
		List<Integration> details = event.getDetails();

		LocalServiceProviderConfiguration serviceProvider = samlConfiguration.getServiceProvider();

		serviceProvider.getProviders().clear();
		serviceProvider.getProviders().addAll(SamlConverter.TO_EXTERNAL_PROVIDER_CONFIG.apply(details));
	}
}
