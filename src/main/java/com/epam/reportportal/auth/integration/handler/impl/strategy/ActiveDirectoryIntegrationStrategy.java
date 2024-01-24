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

import static com.epam.reportportal.auth.integration.converter.LdapConverter.UPDATE_FROM_REQUEST;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.validator.duplicate.IntegrationDuplicateValidator;
import com.epam.reportportal.auth.integration.validator.request.AuthRequestValidator;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
public class ActiveDirectoryIntegrationStrategy extends AuthIntegrationStrategy {

  @Autowired
  public ActiveDirectoryIntegrationStrategy(IntegrationRepository integrationRepository,
      @Qualifier("ldapUpdateAuthRequestValidator")
      AuthRequestValidator<UpdateAuthRQ> updateAuthRequestValidator,
      IntegrationDuplicateValidator integrationDuplicateValidator) {
    super(integrationRepository, updateAuthRequestValidator, integrationDuplicateValidator);
  }

  @Override
  protected void fill(Integration integration, UpdateAuthRQ updateRequest) {
    integration.setName(AuthIntegrationType.ACTIVE_DIRECTORY.getName());
    UPDATE_FROM_REQUEST.accept(updateRequest, integration);
  }

}
