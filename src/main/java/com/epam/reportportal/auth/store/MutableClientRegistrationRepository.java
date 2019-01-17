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
				ErrorType.OAUTH_INTEGRATION_NOT_FOUND,
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
