package com.epam.reportportal.auth.store;

import com.epam.ta.reportportal.dao.OAuthRegistrationRepository;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.reportportal.auth.converter.OAuthRegistrationConverters.TO_SPRING;

public class MutableClientRegistrationRepository implements ClientRegistrationRepository {

	private OAuthRegistrationRepository oAuthRegistrationRepository;

	public MutableClientRegistrationRepository(OAuthRegistrationRepository oAuthRegistrationRepository) {
		this.oAuthRegistrationRepository = oAuthRegistrationRepository;
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		return this.oAuthRegistrationRepository.findById(registrationId).map(TO_SPRING).orElse(null);
	}

	public OAuthRegistration findOAuthRegistrationById(String registrationId) {
		return this.oAuthRegistrationRepository.findById(registrationId).orElse(null);
	}

	public boolean exists(String id) {
		return this.oAuthRegistrationRepository.existsById(id);
	}

	public OAuthRegistration save(OAuthRegistration registration) {
		return this.oAuthRegistrationRepository.save(registration);
	}

	public void delete(String id) {
		oAuthRegistrationRepository.deleteById(id);
	}

	public Collection<OAuthRegistration> findAll() {
		return StreamSupport.stream(this.oAuthRegistrationRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
	}
}
