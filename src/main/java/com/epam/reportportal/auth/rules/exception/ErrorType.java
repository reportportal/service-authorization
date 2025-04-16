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

package com.epam.reportportal.auth.rules.exception;

import lombok.Getter;

/**
 * Report Portal's exception list
 *
 * @author Andrei Varabyeu
 */
@Getter
public enum ErrorType {

  /**
   * Incorrect Report Portal WS Request
   */
  INCORRECT_REQUEST(4001, "Incorrect Request. {}"),

  /**
   * Access Denied
   */
  ACCESS_DENIED(4003, "You do not have enough permissions. {}"),
  /**
   * Access Denied
   */
  ADDRESS_LOCKED(4004, "Address is locked due to several incorrect login attempts"),

  /**
   * If specified by id role not found
   */
  ROLE_NOT_FOUND(4045, "Project role '{}' not found. Did you use correct Role Name?"),

  /**
   * If specified by login User not found
   */
  USER_NOT_FOUND(4046, "User '{}' not found. {}"),

  /**
   * Integration not found
   */
  INTEGRATION_NOT_FOUND(40413, "Integration with ID '{}' not found. Did you use correct ID?"),

  /**
   * If specified Authentication extension isn't found
   */
  AUTH_INTEGRATION_NOT_FOUND(40419, "Auth integration '{}' not found. Did you use correct name?"),

  /**
   * If binary data not found
   */
  UNABLE_TO_LOAD_BINARY_DATA(40427, "Unable to load binary data by id '{}'"),

  /**
   * Unable to recognize provided authentication type
   */
  INCORRECT_AUTHENTICATION_TYPE(40015, "Incorrect authentication type: {}"),

  /**
   * Impossible to interact with integration
   */
  UNABLE_INTERACT_WITH_INTEGRATION(40302, "Impossible interact with integration. {}"),

  /**
   * Unable create duplicate of integration
   */
  INTEGRATION_ALREADY_EXISTS(40910,
      "Integration '{}' already exists. You couldn't create the duplicate."),

  /**
   * Base Error If Request sent with incorrect parameters
   */
  BAD_REQUEST_ERROR(40016, "Error in handled Request. Please, check specified parameters: '{}'"),

  /**
   * If specified attribute not found
   */
  ATTRIBUTE_NOT_FOUND(40017, "Attribute '{}' not found."),

  /**
   * Base ReportPortal Exception. Try to avoid this type and create more custom
   */
  UNCLASSIFIED_REPORT_PORTAL_ERROR(5001, "Unclassified Report Portal Error"),

  /**
   * Use it If there are no any other exceptions. There should by no such exception
   */
  UNCLASSIFIED_ERROR(5000, "Unclassified error");

  private final int code;

  private final String description;

  ErrorType(int code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Get instance by code
   *
   * @param code Error Code
   * @return ErrorType
   */
  public static ErrorType getByCode(int code) {
    for (ErrorType error : values()) {
      if (error.getCode() == code) {
        return error;
      }
    }
    throw new IllegalArgumentException("Unable to find Error with code '" + code + "'");
  }
}
