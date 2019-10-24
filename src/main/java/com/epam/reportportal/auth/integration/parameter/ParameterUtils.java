package com.epam.reportportal.auth.integration.parameter;

import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
				.forEach(it -> expect(StringUtils.isNotBlank((String) request.getAuthParams().get(it)), Predicate.isEqual(true)).verify(
						ErrorType.BAD_REQUEST_ERROR,
						formattedSupplier("parameter '{}' is required.", it)
				));
	}

	public static void validateSamlRequest(UpdateAuthRQ request) {
		Arrays.stream(SamlParameter.values())
				.filter(SamlParameter::isRequired)
				.map(SamlParameter::getParameterName)
				.forEach(it -> expect(StringUtils.isNotBlank((String) request.getAuthParams().get(it)), Predicate.isEqual(true)).verify(
						ErrorType.BAD_REQUEST_ERROR,
						formattedSupplier("parameter '{}' is required.", it)
				));
	}

	public static void setLdapParameters(UpdateAuthRQ request, Integration integration) {
		Arrays.stream(LdapParameter.values()).forEach(it -> it.setParameter(request, integration));
	}

	public static void setSamlParameters(UpdateAuthRQ request, Integration integration) {
		Arrays.stream(SamlParameter.values()).forEach(it -> it.setParameter(request, integration));
	}

	public static Map<String, String> getLdapSyncAttributes(Integration integration) {
		return Arrays.stream(LdapParameter.values())
				.filter(LdapParameter::isSyncAttribute)
				.filter(it -> it.getParameter(integration).isPresent())
				.collect(Collectors.toMap(LdapParameter::getParameterName, it -> it.getParameter(integration).get()));
	}

}
