/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.integration.parameter;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.EMAIL_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.FIRST_NAME_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.FULL_NAME_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_ALIAS;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_METADATA_URL;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_NAME;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_NAME_ID;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.IDP_URL;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.LAST_NAME_ATTRIBUTE;

import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public final class ParameterUtils {

  private ParameterUtils() {
    //static only
  }

  public static void setLdapParameters(UpdateAuthRQ request, Integration integration) {
    Arrays.stream(LdapParameter.values()).forEach(it -> it.setParameter(request, integration));
  }

  public static void setSamlParameters(UpdateAuthRQ request, Integration integration) {
    IDP_NAME.setParameter(request, integration);
    IDP_METADATA_URL.setParameter(request, integration);
    EMAIL_ATTRIBUTE.setParameter(request, integration);
    IDP_NAME_ID.setParameter(request, integration);
    IDP_ALIAS.setParameter(request, integration);
    IDP_URL.setParameter(request, integration);

    FULL_NAME_ATTRIBUTE.getParameter(request).ifPresentOrElse(fullName -> {
      FIRST_NAME_ATTRIBUTE.removeParameter(integration);
      LAST_NAME_ATTRIBUTE.removeParameter(integration);
      FULL_NAME_ATTRIBUTE.setParameter(integration, fullName);
    }, () -> {
      FULL_NAME_ATTRIBUTE.removeParameter(integration);
      FIRST_NAME_ATTRIBUTE.setParameter(request, integration);
      LAST_NAME_ATTRIBUTE.setParameter(request, integration);
    });
  }

  public static Map<String, String> getLdapSyncAttributes(Integration integration) {
    return Arrays.stream(LdapParameter.values())
        .filter(LdapParameter::isSyncAttribute)
        .filter(it -> it.getParameter(integration).isPresent())
        .collect(Collectors.toMap(LdapParameter::getParameterName,
            it -> it.getParameter(integration).get()));
  }

}
