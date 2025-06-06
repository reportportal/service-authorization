/*
 * Copyright 2023 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.auth.entity.activity;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Getter
public enum ActivityAction {

  CREATE_USER("createUser"),
  ASSIGN_USER("assignUser"),
  CREATE_PROJECT("createProject");

  private final String value;

  ActivityAction(String value) {
    this.value = value;
  }

  public static Optional<ActivityAction> fromString(String string) {
    return Optional.ofNullable(string).flatMap(
        str -> Arrays.stream(values()).filter(it -> it.value.equalsIgnoreCase(str)).findAny());
  }

}
