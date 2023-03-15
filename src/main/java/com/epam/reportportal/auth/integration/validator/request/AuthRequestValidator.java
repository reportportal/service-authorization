package com.epam.reportportal.auth.integration.validator.request;

public interface AuthRequestValidator<T> {

  void validate(T authRequest);
}
