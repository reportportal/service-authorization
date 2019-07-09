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
package com.epam.reportportal.auth.oauth;

import com.google.common.base.Preconditions;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Andrei Varabyeu
 */
abstract public class OAuthProvider {

	/**
	 * Is OAuth provider support dynamic configs
	 */
	private final boolean configDynamic;

	/**
	 * Auth provider name
	 */
	private final String name;

	/**
	 * HTML code of button
	 */
	private final String button;

	public OAuthProvider(@Nonnull String name, @Nullable String button, boolean configDynamic) {
		this.name = Preconditions.checkNotNull(name, "Name should not be null");
		this.button = button;
		this.configDynamic = configDynamic;
	}

	public String getName() {
		return name;
	}

	public String getButton() {
		return button;
	}

	public boolean isConfigDynamic() {
		return configDynamic;
	}

	public String buildPath(String basePath) {
		return basePath + (basePath.endsWith("/") ? "" : "/") + this.name;
	}

	public abstract OAuth2RestOperations getOAuthRestOperations(OAuth2ClientContext oauth2ClientContext);

	public abstract ResourceServerTokenServices getTokenServices();
}
