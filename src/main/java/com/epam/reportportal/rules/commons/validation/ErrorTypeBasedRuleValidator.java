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

package com.epam.reportportal.rules.commons.validation;

import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import java.util.function.Predicate;

public class ErrorTypeBasedRuleValidator<T> extends RuleValidator<T> {

  public ErrorTypeBasedRuleValidator(T target, Predicate<T> predicate) {
    super(target, predicate);
  }

  public void verify(ErrorType errorType, Object... args) {
    if (!predicate.test(target)) {
      throw new ReportPortalException(errorType, args);
    }
  }
}