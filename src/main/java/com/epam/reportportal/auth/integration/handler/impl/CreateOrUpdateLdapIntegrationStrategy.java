package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateOrUpdateIntegrationStrategy;
import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import com.epam.reportportal.auth.integration.parameter.ParameterUtils;
import com.epam.reportportal.auth.util.Encryptor;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Component
public class CreateOrUpdateLdapIntegrationStrategy extends CreateOrUpdateIntegrationStrategy {

	private final Encryptor encryptor;

	public CreateOrUpdateLdapIntegrationStrategy(IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository, Encryptor encryptor) {
		super(integrationTypeRepository, integrationRepository);
		this.encryptor = encryptor;
		this.type = AuthIntegrationType.LDAP;
	}

	@Override
	protected void preProcess(Integration integration) {

	}

	@Override
	protected Optional<Integration> find(UpdateAuthRQ request, IntegrationType type) {
		return integrationRepository.findExclusiveAuth(this.type.getName());
	}

	@Override
	protected void postProcess(Integration integration) {

	}

	@Override
	protected void beforeUpdate(UpdateAuthRQ request, Integration integration) {
		ParameterUtils.validateLdapRequest(request);
		Optional<String> fromRq = LdapParameter.MANAGER_PASSWORD.getParameter(request);
		Optional<String> stored = LdapParameter.MANAGER_PASSWORD.getParameter(integration);
		if (stored.isPresent()) {
			if (fromRq.isPresent() && stored.get().equals(fromRq.get()) && !encryptor.decrypt(stored.get()).equals(fromRq.get())) {
				LdapParameter.MANAGER_PASSWORD.setParameter(integration, encryptor.encrypt(fromRq.get()));
			} else {
				LdapParameter.MANAGER_PASSWORD.setParameter(integration, stored.get());
			}
		}
	}

	@Override
	protected void beforeCreate(UpdateAuthRQ request) {
		ParameterUtils.validateLdapRequest(request);
		LdapParameter.MANAGER_PASSWORD.getParameter(request)
				.ifPresent(it -> request.getAuthParams().put(LdapParameter.MANAGER_PASSWORD.getParameterName(), encryptor.encrypt(it)));
	}
}
