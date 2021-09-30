package com.epam.reportportal.auth.integration.validator.request;

import com.epam.reportportal.auth.integration.validator.request.param.provider.ParamNamesProvider;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static com.epam.ta.reportportal.commons.validation.Suppliers.formattedSupplier;
import static java.util.Optional.ofNullable;

public class UpdateAuthRequestValidator implements AuthRequestValidator<UpdateAuthRQ> {

	private final ParamNamesProvider paramNamesProvider;

	public UpdateAuthRequestValidator(ParamNamesProvider paramNamesProvider) {
		this.paramNamesProvider = paramNamesProvider;
	}

	@Override
	public void validate(UpdateAuthRQ updateRequest) {
		final List<String> paramNames = paramNamesProvider.provide();
		paramNames.stream()
				.map(it -> retrieveParam(updateRequest, it))
				.forEach(it -> expect(it, Optional::isPresent).verify(ErrorType.BAD_REQUEST_ERROR,
						formattedSupplier("parameter '{}' is required.", it)
				));
	}

	private Optional<String> retrieveParam(UpdateAuthRQ updateRequest, String name) {
		return ofNullable(updateRequest.getIntegrationParams().get(name)).map(String::valueOf).filter(StringUtils::isNotBlank);
	}
}
