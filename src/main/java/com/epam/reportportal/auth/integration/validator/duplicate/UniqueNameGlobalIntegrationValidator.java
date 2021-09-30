package com.epam.reportportal.auth.integration.validator.duplicate;

import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UniqueNameGlobalIntegrationValidator implements IntegrationDuplicateValidator {

	private final IntegrationRepository integrationRepository;

	@Autowired
	public UniqueNameGlobalIntegrationValidator(IntegrationRepository integrationRepository) {
		this.integrationRepository = integrationRepository;
	}

	@Override
	public void validate(Integration integration) {
		integrationRepository.findByNameAndTypeIdAndProjectIdIsNull(integration.getName(), integration.getType().getId())
				.ifPresent(found -> BusinessRule.expect(found.getId(), id -> id.equals(integration.getId()))
						.verify(ErrorType.INTEGRATION_ALREADY_EXISTS, integration.getName()));
	}
}
