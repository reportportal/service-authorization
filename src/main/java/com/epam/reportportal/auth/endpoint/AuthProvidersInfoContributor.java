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
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.oauth.OAuthProvider;
import com.epam.ta.reportportal.dao.OAuthRegistrationRepository;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.config.SecurityConfiguration.GlobalWebSecurityConfig.SSO_LOGIN_PATH;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

/**
 * Shows list of supported authentication providers
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class AuthProvidersInfoContributor implements InfoContributor {

	private final OAuthRegistrationRepository oAuthRegistrationRepository;
	private final Map<String, OAuthProvider> providersMap;

	@Autowired
	public AuthProvidersInfoContributor(OAuthRegistrationRepository oAuthRegistrationRepository, Map<String, OAuthProvider> providersMap) {
		this.oAuthRegistrationRepository = oAuthRegistrationRepository;
		this.providersMap = providersMap;
	}

	@Override
	public void contribute(Info.Builder builder) {
		final List<OAuthRegistration> oauth2Details = oAuthRegistrationRepository.findAll();

		final Map<String, AuthProviderInfo> providers = providersMap.values()
				.stream()
				.filter(p -> !p.isConfigDynamic() || oauth2Details.stream().anyMatch(it -> it.getId().equalsIgnoreCase(p.getName())))
				.collect(Collectors.toMap(OAuthProvider::getName,
						p -> new AuthProviderInfo(p.getButton(), p.buildPath(getAuthBasePath()))
				));

		builder.withDetail("auth_extensions", providers);
	}

	private String getAuthBasePath() {
		return fromCurrentContextPath().path(SSO_LOGIN_PATH).build().getPath();
	}

	public static class AuthProviderInfo {
		private String button;
		private String path;

		public AuthProviderInfo(String button, String path) {
			this.button = button;
			this.path = path;
		}

		public String getButton() {
			return button;
		}

		public void setButton(String button) {
			this.button = button;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

}
