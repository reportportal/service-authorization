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

import static com.epam.reportportal.auth.integration.converter.SamlConverter.UPDATE_FROM_REQUEST;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.BASE_PATH;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_ALIAS;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_NAME;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_NAME_ID;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_URL;
import static java.util.Optional.ofNullable;

import com.epam.reportportal.auth.dao.IntegrationRepository;
import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import com.epam.reportportal.auth.entity.integration.IntegrationTypeDetails;
import com.epam.reportportal.auth.event.SamlProvidersReloadEvent;
import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import com.epam.reportportal.auth.integration.validator.duplicate.IntegrationDuplicateValidator;
import com.epam.reportportal.auth.integration.validator.request.AuthRequestValidator;
import com.epam.reportportal.auth.model.integration.auth.UpdateAuthRQ;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.opensaml.saml.saml2.core.NameID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
public class SamlIntegrationStrategy extends AuthIntegrationStrategy {
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public SamlIntegrationStrategy(IntegrationRepository integrationRepository,
      @Qualifier("samlUpdateAuthRequestValidator")
      AuthRequestValidator<UpdateAuthRQ> updateAuthRequestValidator,
      IntegrationDuplicateValidator integrationDuplicateValidator,
      ApplicationEventPublisher eventPublisher) {
    super(integrationRepository, updateAuthRequestValidator, integrationDuplicateValidator);
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected void fill(Integration integration, UpdateAuthRQ updateRequest) {
    UPDATE_FROM_REQUEST.accept(updateRequest, integration);
    String baseURL = getBaseUrl();
    updateBasePath(integration, baseURL);
  }

  public String getBaseUrl() {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();

    return request.getRequestURL().toString().replace(request.getRequestURI(), "");
  }

  private void updateBasePath(Integration integration, String basePath) {
    final IntegrationType integrationType = integration.getType();
    String idpName = IDP_NAME.getParameter(integration).orElse("").replace(" ", "%20");
    final IntegrationTypeDetails typeDetails = ofNullable(integrationType.getDetails()).orElseGet(
        () -> {
          final IntegrationTypeDetails details = new IntegrationTypeDetails();
          integrationType.setDetails(details);
          return details;
        });
    final Map<String, Object> detailsMapping = ofNullable(typeDetails.getDetails()).orElseGet(
        () -> {
          final Map<String, Object> details = new HashMap<>();
          typeDetails.setDetails(details);
          return details;
        });
    String callbackURL = basePath + "/login/saml2/sso/" + idpName;
    detailsMapping.put(BASE_PATH.getParameterName(), callbackURL);
  }

  @Override
  protected Integration save(Integration integration) {
    populateProviderDetails(integration);
    final Integration result = super.save(integration);
    eventPublisher.publishEvent(new SamlProvidersReloadEvent(result.getType()));
    return result;
  }

  private void populateProviderDetails(Integration samlIntegration) {
    Map<String, Object> params = samlIntegration.getParams().getParams();
    String metadataUrl = SamlParameter.IDP_METADATA_URL.getRequiredParameter(samlIntegration);

    RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
        .fromMetadataLocation(metadataUrl)
        .build();

    params.put(IDP_URL.getParameterName(), relyingPartyRegistration.getRegistrationId());
    params.put(IDP_ALIAS.getParameterName(), relyingPartyRegistration.getAssertingPartyDetails().getEntityId());
    params.put(IDP_NAME_ID.getParameterName(), StringUtils.isNotEmpty(relyingPartyRegistration.getNameIdFormat()) ? relyingPartyRegistration.getNameIdFormat() : NameID.UNSPECIFIED);
  }
}
