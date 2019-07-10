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

package com.epam.reportportal.auth.store;

import com.epam.ta.reportportal.commons.validation.Suppliers;
import com.epam.ta.reportportal.dao.OAuthRegistrationRepository;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

import static com.epam.reportportal.auth.integration.converter.OAuthRegistrationConverters.TO_SPRING;

@Component("mutableClientRegistrationRepository")
public class MutableClientRegistrationRepository implements ClientRegistrationRepository {

	private final OAuthRegistrationRepository oAuthRegistrationRepository;

	@Autowired
	public MutableClientRegistrationRepository(OAuthRegistrationRepository oAuthRegistrationRepository) {
		this.oAuthRegistrationRepository = oAuthRegistrationRepository;
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		return this.oAuthRegistrationRepository.findById(registrationId).map(TO_SPRING).orElseThrow(() -> new ReportPortalException(
				ErrorType.AUTH_INTEGRATION_NOT_FOUND,
				Suppliers.formattedSupplier("Client registration with id = {} has not been found.", registrationId).get()
		));
	}

	public Optional<OAuthRegistration> findOAuthRegistrationById(String registrationId) {
		return this.oAuthRegistrationRepository.findById(registrationId);
	}

	public boolean existsById(String oauthProviderId) {
		return this.oAuthRegistrationRepository.existsById(oauthProviderId);
	}

	public OAuthRegistration save(OAuthRegistration registration) {
		return this.oAuthRegistrationRepository.save(registration);
	}

	public void deleteById(String oauthProviderId) {
		oAuthRegistrationRepository.deleteById(oauthProviderId);
	}

	public Collection<OAuthRegistration> findAll() {
		return oAuthRegistrationRepository.findAll();
	}
}
