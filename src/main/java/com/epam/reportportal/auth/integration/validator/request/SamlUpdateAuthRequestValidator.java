package com.epam.reportportal.auth.integration.validator.request;

import static com.epam.reportportal.auth.integration.parameter.SamlParameter.FIRST_NAME_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.FULL_NAME_ATTRIBUTE;
import static com.epam.reportportal.auth.integration.parameter.SamlParameter.LAST_NAME_ATTRIBUTE;
import static com.epam.ta.reportportal.commons.Predicates.equalTo;

import com.epam.reportportal.auth.integration.validator.request.param.provider.ParamNamesProvider;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateAuthRQ;
import java.util.function.Predicate;

public class SamlUpdateAuthRequestValidator extends UpdateAuthRequestValidator {

  private static final Predicate<UpdateAuthRQ> FULL_NAME_IS_EMPTY = request -> FULL_NAME_ATTRIBUTE.getParameter(
      request).isEmpty();
  private static final Predicate<UpdateAuthRQ> FIRST_AND_LAST_NAME_IS_EMPTY = request ->
      LAST_NAME_ATTRIBUTE.getParameter(request).isEmpty() && FIRST_NAME_ATTRIBUTE.getParameter(
          request).isEmpty();

  public SamlUpdateAuthRequestValidator(ParamNamesProvider paramNamesProvider) {
    super(paramNamesProvider);
  }

  @Override
  public void validate(UpdateAuthRQ updateRequest) {
    super.validate(updateRequest);
    BusinessRule.expect(
        FULL_NAME_IS_EMPTY.test(updateRequest) && FIRST_AND_LAST_NAME_IS_EMPTY.test(updateRequest),
        equalTo(Boolean.FALSE)
    ).verify(ErrorType.BAD_REQUEST_ERROR,
        "Fields Full name or combination of Last name and First name are empty");
  }
}
