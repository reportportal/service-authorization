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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.reportportal.auth.converter.OAuthRegistrationConverters.TO_SPRING;

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
				ErrorType.OAUTH_INTEGRATION_NOT_FOUND,
				Suppliers.formattedSupplier("Client registration with id = {} has not been found.", registrationId).get()
		));
	}

	public OAuthRegistration findOAuthRegistrationById(String registrationId) {
		return this.oAuthRegistrationRepository.findById(registrationId).orElseThrow(() -> new ReportPortalException(
				ErrorType.OAUTH_INTEGRATION_NOT_FOUND,
				Suppliers.formattedSupplier("Oauth settings with id = {} have not been found.", registrationId).get()
		));
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
		return StreamSupport.stream(this.oAuthRegistrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}
}
