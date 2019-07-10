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
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.dao.IntegrationRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Dynamic (enableable) auth provider
 *
 * @author Andrei Varabyeu
 */
public abstract class EnableableAuthProvider implements AuthenticationProvider {

	protected final IntegrationRepository integrationRepository;

	protected EnableableAuthProvider(IntegrationRepository integrationRepository) {
		this.integrationRepository = integrationRepository;
	}

	protected abstract boolean isEnabled();

	protected abstract AuthenticationProvider getDelegate();

	@Override
	public final Authentication authenticate(Authentication authentication) throws AuthenticationException {
		return isEnabled() ? getDelegate().authenticate(authentication) : null;
	}

	@Override
	public final boolean supports(Class<?> authentication) {
		return isEnabled() && getDelegate().supports(authentication);
	}

}