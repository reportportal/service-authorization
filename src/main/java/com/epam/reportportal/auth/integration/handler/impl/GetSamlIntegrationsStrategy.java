package com.epam.reportportal.auth.integration.handler.impl;

import static com.epam.reportportal.auth.integration.converter.SamlConverter.TO_PROVIDERS_RESOURCE;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationStrategy;
import com.epam.reportportal.auth.model.SamlProvidersResource;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.model.integration.auth.AbstractAuthResource;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
public class GetSamlIntegrationsStrategy implements GetAuthIntegrationStrategy {

  private IntegrationTypeRepository integrationTypeRepository;

  private IntegrationRepository integrationRepository;

  @Autowired
  public GetSamlIntegrationsStrategy(IntegrationTypeRepository integrationTypeRepository,
      IntegrationRepository integrationRepository) {
    this.integrationTypeRepository = integrationTypeRepository;
    this.integrationRepository = integrationRepository;
  }

  @Override
  public AbstractAuthResource getIntegration() {
    IntegrationType samlIntegrationType = integrationTypeRepository.findByName(
            AuthIntegrationType.SAML.getName())
        .orElseThrow(() -> new ReportPortalException(ErrorType.AUTH_INTEGRATION_NOT_FOUND,
            AuthIntegrationType.SAML.getName()));
    List<Integration> providers = integrationRepository.findAllGlobalByType(samlIntegrationType);
    SamlProvidersResource resource = TO_PROVIDERS_RESOURCE.apply(providers);
    resource.setType(samlIntegrationType.getName());
    return resource;
  }
}
