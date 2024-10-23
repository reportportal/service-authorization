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

package com.epam.reportportal.auth.integration.saml;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents attributes extracted from SAML response message.
 *
 * @author Yevgeniy Svalukhin
 */
@EqualsAndHashCode
@Getter
public class Attribute implements Serializable {

  private static final long serialVersionUID = -182983902349882L;

  private String name;
  private String friendlyName;
  private List<Object> values = new LinkedList<>();
  private String nameFormat = "";
  private boolean required;


  public Attribute withName(String name) {
    this.name = name;
    return this;
  }

  public Attribute withFriendlyName(String friendlyName) {
    this.friendlyName = friendlyName;
    return this;
  }

  public Attribute withValues(List<Object> values) {
    this.values.addAll(values);
    return this;
  }

  public Attribute withNameFormat(String nameFormat) {
    this.nameFormat = nameFormat;
    return this;
  }

  public Attribute withRequired(boolean required) {
    this.required = required;
    return this;
  }

}
