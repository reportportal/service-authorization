package com.epam.reportportal.auth.integration.provider;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.impl.strategy.AuthIntegrationStrategy;

import java.util.Map;
import java.util.Optional;

public class AuthIntegrationStrategyProvider {

	private final Map<AuthIntegrationType, AuthIntegrationStrategy> authIntegrationStrategyMapping;

	public AuthIntegrationStrategyProvider(Map<AuthIntegrationType, AuthIntegrationStrategy> authIntegrationStrategyMapping) {
		this.authIntegrationStrategyMapping = authIntegrationStrategyMapping;
	}

	public Optional<AuthIntegrationStrategy> provide(AuthIntegrationType type) {
		return Optional.ofNullable(authIntegrationStrategyMapping.get(type));
	}
}
