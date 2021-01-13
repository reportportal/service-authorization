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
package com.epam.reportportal.auth.integration.ldap;

import com.epam.reportportal.auth.EnableableAuthProvider;
import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

/**
 * Active Directory provider
 *
 * @author Andrei Varabyeu
 */
public class ActiveDirectoryAuthProvider extends EnableableAuthProvider {

	private final DetailsContextMapper detailsContextMapper;

	public ActiveDirectoryAuthProvider(IntegrationRepository integrationRepository, ApplicationEventPublisher eventPublisher,
			DetailsContextMapper detailsContextMapper) {
		super(integrationRepository, eventPublisher);
		this.detailsContextMapper = detailsContextMapper;
	}

	@Override
	protected boolean isEnabled() {
		return integrationRepository.findAllByTypeIn(AuthIntegrationType.ACTIVE_DIRECTORY.getName()).stream().findFirst().isPresent();
	}

	@Override
	protected AuthenticationProvider getDelegate() {

		Integration integration = integrationRepository.findAllByTypeIn(AuthIntegrationType.ACTIVE_DIRECTORY.getName())
				.stream()
				.findFirst()
				.orElseThrow(() -> new BadCredentialsException("Active Directory is not configured"));

		ActiveDirectoryLdapAuthenticationProvider adAuth = new ActiveDirectoryLdapAuthenticationProvider(LdapParameter.DOMAIN.getParameter(
				integration).orElse(null),
				LdapParameter.URL.getRequiredParameter(integration),
				LdapParameter.BASE_DN.getRequiredParameter(integration)
		);

		adAuth.setAuthoritiesMapper(new NullAuthoritiesMapper());
		adAuth.setUserDetailsContextMapper(detailsContextMapper);
		return adAuth;
	}
}
