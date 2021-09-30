package com.epam.reportportal.auth.integration.validator.duplicate;

import com.epam.ta.reportportal.entity.integration.Integration;

public interface IntegrationDuplicateValidator {

	void validate(Integration integration);
}
