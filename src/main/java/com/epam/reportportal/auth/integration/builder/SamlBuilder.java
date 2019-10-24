package com.epam.reportportal.auth.integration.builder;

import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;

import static com.epam.reportportal.auth.integration.converter.SamlConverter.UPDATE_FROM_REQUEST;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class SamlBuilder extends AuthIntegrationBuilder {

	public SamlBuilder() {
		super();
	}

	@Override
	public AuthIntegrationBuilder addUpdateRq(UpdateAuthRQ request) {
		UPDATE_FROM_REQUEST.apply(request, integration);
		return this;
	}
}
