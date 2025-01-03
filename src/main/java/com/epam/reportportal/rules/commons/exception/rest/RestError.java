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

import com.epam.reportportal.rules.exception.ErrorRS;
import com.epam.reportportal.rules.exception.ErrorType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rest Error representation. Contains rest error template and real exception data.
 *
 * @author Andrei Varabyeu
 */
@Getter
public class RestError {

  private final ErrorRS errorRs;

  /**
   * HTTP Status
   */
  private final HttpStatus httpStatus;

  public RestError(HttpStatus httpStatus, ErrorRS errorRs) {
    this.httpStatus = httpStatus;
    this.errorRs = errorRs;
  }

  /**
   * Builder for Rest Error.
   *
   * @author Andrei Varabyeu
   */
  public static class Builder {

    private HttpStatus status;
    private ErrorType error;
    private String message;
    private String stackTrace;

    public Builder setStatus(HttpStatus status) {
      this.status = status;
      return this;
    }

    public Builder setError(ErrorType error) {
      this.error = error;
      return this;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setStackTrace(String stackTrace) {
      this.stackTrace = stackTrace;
      return this;
    }

    /**
     * Builds the RestError object.
     *
     * @return a new instance of RestError
     */
    public RestError build() {
      ErrorRS errorRS = new ErrorRS();
      errorRS.setMessage(message);
      errorRS.setStackTrace(stackTrace);
      errorRS.setErrorType(error);

      return new RestError(status, errorRS);
    }
  }
}
