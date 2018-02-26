package com.epam.reportportal.auth.integration;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class MutableClientRegistrationRepository implements ClientRegistrationRepository {
	public static final Collector<ClientRegistration, ?, Map<String, ClientRegistration>> KEY_MAPPER = Collectors.toMap(ClientRegistration::getRegistrationId,
			r -> r
	);
	private final Map<String, ClientRegistration> registrations;

	public MutableClientRegistrationRepository() {
		this.registrations = new HashMap<>();
	}

	public MutableClientRegistrationRepository(ClientRegistration... registrations) {
		this.registrations = Arrays.stream(registrations).collect(KEY_MAPPER);
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		return registrations.get(registrationId);
	}

	public boolean exists(String id) {
		return registrations.containsKey(id);

	}

	public ClientRegistration save(ClientRegistration registration) {
		registrations.put(registration.getRegistrationId(), registration);
		return registration;
	}

	public boolean delete(String id) {
		return null != registrations.remove(id);
	}

	public Collection<ClientRegistration> findAll() {
		return registrations.values();
	}
}
