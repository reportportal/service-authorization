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

package com.epam.reportportal.rules.commons.exception.rest;

import com.epam.reportportal.rules.commons.exception.message.ExceptionMessageBuilder;
import com.epam.reportportal.rules.exception.ErrorType;
import org.springframework.http.HttpStatus;

/**
 * REST Error template. Created to be able to configure error templates in Spring's IoC container
 *
 * @author Andrei Varabyeu
 */
public class RestErrorDefinition<T extends Exception> {

  private final HttpStatus httpStatus;
  private final ErrorType error;
  private final ExceptionMessageBuilder<T> exceptionMessageBuilder;

  public RestErrorDefinition(HttpStatus httpStatus, ErrorType error,
      ExceptionMessageBuilder<T> exceptionMessageBuilder) {
    super();
    this.httpStatus = httpStatus;
    this.error = error;
    this.exceptionMessageBuilder = exceptionMessageBuilder;
  }

  public RestErrorDefinition(int httpStatus, ErrorType error,
      ExceptionMessageBuilder<T> exceptionMessageBuilder) {
    this(HttpStatus.valueOf(httpStatus), error, exceptionMessageBuilder);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public ErrorType getError() {
    return error;
  }

  public String getExceptionMessage(T e) {
    return exceptionMessageBuilder.buildMessage(e);
  }

  public ExceptionMessageBuilder<? extends Exception> getExceptionMessageBuilder() {
    return exceptionMessageBuilder;
  }
}