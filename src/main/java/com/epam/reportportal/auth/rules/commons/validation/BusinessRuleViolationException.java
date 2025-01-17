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

package com.epam.reportportal.auth.rules.commons.validation;


import static com.epam.reportportal.auth.rules.commons.validation.Suppliers.trimMessage;
import static com.epam.reportportal.auth.rules.exception.ReportPortalException.MAX_ERROR_MESSAGE_LENGTH;

public class BusinessRuleViolationException extends Exception {

  public BusinessRuleViolationException(String message) {
    super(trimMessage(message, MAX_ERROR_MESSAGE_LENGTH));
  }

}
