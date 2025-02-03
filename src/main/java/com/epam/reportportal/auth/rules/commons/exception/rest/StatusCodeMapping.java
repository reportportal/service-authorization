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

package com.epam.reportportal.auth.rules.commons.exception.rest;

import com.epam.reportportal.auth.rules.exception.ErrorType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * {@link ErrorType} to {@link HttpStatus} mapping.
 *
 * @author Andrei Varabyeu
 */
public class StatusCodeMapping {

  private StatusCodeMapping() {

  }

  private static final Map<ErrorType, HttpStatus> MAPPING = new HashMap<>() {
    private static final long serialVersionUID = 1L;

    {
      put(ErrorType.ROLE_NOT_FOUND, HttpStatus.NOT_FOUND);
      put(ErrorType.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
      put(ErrorType.AUTH_INTEGRATION_NOT_FOUND, HttpStatus.NOT_FOUND);
      put(ErrorType.UNABLE_TO_LOAD_BINARY_DATA, HttpStatus.NOT_FOUND);

      // ExternalSystem related
      put(ErrorType.INTEGRATION_NOT_FOUND, HttpStatus.NOT_FOUND);
      put(ErrorType.INTEGRATION_ALREADY_EXISTS, HttpStatus.CONFLICT);
      put(ErrorType.INCORRECT_AUTHENTICATION_TYPE, HttpStatus.BAD_REQUEST);
      put(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION, HttpStatus.CONFLICT);

      /* Authentication related */
      put(ErrorType.ACCESS_DENIED, HttpStatus.FORBIDDEN);
      put(ErrorType.ADDRESS_LOCKED, HttpStatus.FORBIDDEN);

      put(ErrorType.INCORRECT_REQUEST, HttpStatus.BAD_REQUEST);
      put(ErrorType.BAD_REQUEST_ERROR, HttpStatus.BAD_REQUEST);
      put(ErrorType.UNCLASSIFIED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      put(ErrorType.UNCLASSIFIED_REPORT_PORTAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

    }
  };

  public static HttpStatus getHttpStatus(ErrorType errorType, HttpStatus defaultStatus) {
    return getHttpStatus(errorType).orElse(defaultStatus);
  }

  public static Optional<HttpStatus> getHttpStatus(ErrorType errorType) {
    return Optional.ofNullable(MAPPING.get(errorType));
  }

}
