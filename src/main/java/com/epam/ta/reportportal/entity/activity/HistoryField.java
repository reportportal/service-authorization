/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.ta.reportportal.entity.activity;

import com.epam.ta.reportportal.commons.JsonbUserType;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Ihar Kahadouski
 */
@Setter
@Getter
public class HistoryField extends JsonbUserType implements Serializable {

  private String field;
  private String oldValue;
  private String newValue;

  public HistoryField() {
  }

  public HistoryField(String field, String oldValue, String newValue) {
    this.field = field;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public static HistoryField of(String field, String oldValue, String newValue) {
    return new HistoryField(field, oldValue, newValue);
  }

  @Override
  public Class<?> returnedClass() {
    return HistoryField.class;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HistoryField that = (HistoryField) o;
    return Objects.equals(field, that.field) && Objects.equals(oldValue, that.oldValue)
        && Objects.equals(newValue, that.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, oldValue, newValue);
  }

  @Override
  public String toString() {
    return "HistoryField{" + "field='" + field + '\''
        + ", oldValue='" + oldValue + '\''
        + ", newValue='" + newValue + '\''
        + '}';
  }
}
