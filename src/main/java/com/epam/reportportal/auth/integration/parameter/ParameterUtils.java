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

import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.*;
import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static com.epam.ta.reportportal.commons.validation.Suppliers.formattedSupplier;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public final class ParameterUtils {

	private ParameterUtils() {
		//static only
	}

	public static void validateLdapRequest(UpdateAuthRQ request) {
		Arrays.stream(LdapParameter.values())
				.filter(LdapParameter::isRequired)
				.map(LdapParameter::getParameterName)
				.forEach(it -> expect(StringUtils.isNotBlank((String) request.getIntegrationParams().get(it)),
						Predicate.isEqual(true)
				).verify(ErrorType.BAD_REQUEST_ERROR, formattedSupplier("parameter '{}' is required.", it)));
	}

	public static void validateSamlRequest(UpdateAuthRQ request) {
		Arrays.stream(SamlParameter.values())
				.filter(SamlParameter::isRequired)
				.map(SamlParameter::getParameterName)
				.forEach(it -> expect(StringUtils.isNotBlank((String) request.getIntegrationParams().get(it)),
						Predicate.isEqual(true)
				).verify(ErrorType.BAD_REQUEST_ERROR, formattedSupplier("parameter '{}' is required.", it)));
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
				.collect(Collectors.toMap(LdapParameter::getParameterName, it -> it.getParameter(integration).get()));
	}

}
