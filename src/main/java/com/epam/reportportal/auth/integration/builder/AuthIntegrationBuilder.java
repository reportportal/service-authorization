package com.epam.reportportal.auth.integration.builder;

import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public abstract class AuthIntegrationBuilder {

	protected final Integration integration;

	public AuthIntegrationBuilder(){
		integration = new Integration();
		integration.setCreationDate(LocalDateTime.now());
	}

	public AuthIntegrationBuilder(Integration integration) {
		this.integration = integration;
	}

	public AuthIntegrationBuilder addCreator(String username) {
		integration.setCreator(username);
		return this;
	}

	public AuthIntegrationBuilder addIntegrationType(IntegrationType type) {
		integration.setType(type);
		return this;
	}

	public abstract AuthIntegrationBuilder addUpdateRq(UpdateAuthRQ request);

	public @NotNull Integration build() {
		return integration;
	}
}
