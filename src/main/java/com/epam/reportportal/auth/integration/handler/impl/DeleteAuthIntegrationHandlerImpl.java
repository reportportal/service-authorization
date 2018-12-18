package com.epam.reportportal.auth.integration.handler.impl;

import com.epam.reportportal.auth.integration.handler.DeleteAuthIntegrationHandler;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.enums.IntegrationGroupEnum;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Service
public class DeleteAuthIntegrationHandlerImpl implements DeleteAuthIntegrationHandler {

	private final IntegrationRepository integrationRepository;

	@Autowired
	public DeleteAuthIntegrationHandlerImpl(IntegrationRepository integrationRepository) {
		this.integrationRepository = integrationRepository;
	}

	@Override
	public OperationCompletionRS deleteAuthIntegrationById(Long integrationId) {
		Integration integration = integrationRepository.findById(integrationId)
				.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, integrationId));

		BusinessRule.expect(integration.getType().getIntegrationGroup(), equalTo(IntegrationGroupEnum.AUTH))
				.verify(ErrorType.BAD_REQUEST_ERROR, "Integration should have an 'AUTH' integration group type.");

		integrationRepository.deleteById(integrationId);

		return new OperationCompletionRS("Auth integration with id= " + integrationId + " has been successfully removed.");

	}
}
