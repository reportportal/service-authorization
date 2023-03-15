/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.integration.handler.impl.strategy;

import com.epam.reportportal.auth.integration.builder.AuthIntegrationBuilder;
import com.epam.reportportal.auth.integration.validator.duplicate.IntegrationDuplicateValidator;
import com.epam.reportportal.auth.integration.validator.request.AuthRequestValidator;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public abstract class AuthIntegrationStrategy {

  private final IntegrationRepository integrationRepository;
  private final AuthRequestValidator<UpdateAuthRQ> updateAuthRequestValidator;
  private final IntegrationDuplicateValidator integrationDuplicateValidator;

  public AuthIntegrationStrategy(IntegrationRepository integrationRepository,
      AuthRequestValidator<UpdateAuthRQ> updateAuthRequestValidator,
      IntegrationDuplicateValidator integrationDuplicateValidator) {
    this.integrationRepository = integrationRepository;
    this.updateAuthRequestValidator = updateAuthRequestValidator;
    this.integrationDuplicateValidator = integrationDuplicateValidator;
  }

  protected abstract void fill(Integration integration, UpdateAuthRQ updateRequest);

  public Integration createIntegration(IntegrationType integrationType, UpdateAuthRQ request,
      String username) {
    updateAuthRequestValidator.validate(request);

    final Integration integration = new AuthIntegrationBuilder().addCreator(username)
        .addIntegrationType(integrationType)
        .addCreationDate(LocalDateTime.now(ZoneOffset.UTC))
        .build();
    fill(integration, request);

    return save(integration);
  }

  public Integration updateIntegration(IntegrationType integrationType, Long integrationId,
      UpdateAuthRQ request) {
    updateAuthRequestValidator.validate(request);

    final Integration integration = integrationRepository.findByIdAndTypeIdAndProjectIdIsNull(
            integrationId, integrationType.getId())
        .orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND,
            integrationType.getName()));
    fill(integration, request);

    return save(integration);
  }

  protected Integration save(Integration integration) {
    integrationDuplicateValidator.validate(integration);
    return integrationRepository.save(integration);
  }

}
