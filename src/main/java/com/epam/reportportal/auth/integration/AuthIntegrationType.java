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
package com.epam.reportportal.auth.integration;

import static java.util.Optional.ofNullable;

import com.epam.reportportal.auth.integration.converter.ActiveDirectoryConverter;
import com.epam.reportportal.auth.integration.converter.LdapConverter;
import com.epam.reportportal.auth.integration.converter.SamlConverter;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.SamlResource;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Andrei Varabyeu
 */
public enum AuthIntegrationType {

  ACTIVE_DIRECTORY("ad") {
    @Override
    public Optional<Integration> get(Integration entity) {
      return ofNullable(entity);
    }

    @Override
    public Function<Integration, ActiveDirectoryResource> getToResourceMapper() {
      return ActiveDirectoryConverter.TO_RESOURCE;
    }
  },
  LDAP("ldap") {
    @Override
    public Optional<Integration> get(Integration entity) {
      return ofNullable(entity);
    }

    @Override
    public Function<Integration, LdapResource> getToResourceMapper() {
      return LdapConverter.TO_RESOURCE;
    }
  },
  SAML("saml") {
    @Override
    public Optional<Integration> get(Integration entity) {
      return ofNullable(entity);
    }

    @Override
    public Function<Integration, SamlResource> getToResourceMapper() {
      return SamlConverter.TO_RESOURCE;
    }
  };

  private String name;

  AuthIntegrationType(String name) {
    this.name = name;
  }

  public static Optional<AuthIntegrationType> fromId(String id) {
    return Arrays.stream(values()).filter(it -> it.name.equalsIgnoreCase(id)).findAny();
  }

  public abstract Optional<Integration> get(Integration entity);

  public abstract Function<Integration, ? extends AbstractAuthResource> getToResourceMapper();

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
