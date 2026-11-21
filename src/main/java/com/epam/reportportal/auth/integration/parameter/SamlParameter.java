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

package com.epam.reportportal.auth.integration.parameter;

import static com.epam.reportportal.auth.rules.commons.validation.Suppliers.formattedSupplier;
import static java.util.Optional.ofNullable;

import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationParams;
import com.epam.reportportal.auth.model.integration.auth.UpdateAuthRQ;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public enum SamlParameter {

  IDP_NAME("identityProviderName", true),
  IDP_METADATA_URL("identityProviderMetadataUrl", true),
  EMAIL_ATTRIBUTE("emailAttribute", true),
  IDP_NAME_ID("identityProviderNameId", false),
  IDP_ALIAS("identityProviderAlias", false),
  IDP_URL("identityProviderUrl", false),
  FULL_NAME_ATTRIBUTE("fullNameAttribute", false),
  FIRST_NAME_ATTRIBUTE("firstNameAttribute", false),
  LAST_NAME_ATTRIBUTE("lastNameAttribute", false),
  ROLES_ATTRIBUTE("rolesAttribute", false),
  /**
   * Optional list of role attribute values that grant the administrator role. Unlike the other
   * parameters it is stored as a JSON array of strings; use {@link #getAdminRoleValues} and
   * {@link #normalizeAdminRoleValues} instead of the string accessors.
   */
  ADMIN_ROLE_VALUES("adminRoleValues", false);

  /**
   * Maximum number of configured administrator role values.
   */
  public static final int MAX_ADMIN_ROLE_VALUES = 50;

  /**
   * Maximum length of a single administrator role value.
   */
  public static final int MAX_ADMIN_ROLE_VALUE_LENGTH = 255;

  /**
   * Role value that grants the administrator role when no values are configured.
   */
  public static final String DEFAULT_ADMIN_ROLE_VALUE = "ADMINISTRATOR";

  private String parameterName;

  private boolean required;

  SamlParameter(String parameterName, boolean required) {
    this.parameterName = parameterName;
    this.required = required;
  }

  public String getParameterName() {
    return parameterName;
  }

  public boolean isRequired() {
    return required;
  }

  public Optional<String> getParameter(Integration integration) {
    return ofNullable((String) integration.getParams().getParams().get(parameterName));
  }

  public Optional<String> getParameter(Map<String, Object> parameterMap) {
    return ofNullable(parameterMap.get(parameterName)).map(it -> (String) it)
        .filter(StringUtils::isNotBlank);
  }

  public Optional<String> getParameter(UpdateAuthRQ request) {
    return ofNullable(request.getIntegrationParams()).flatMap(this::getParameter);
  }

  public void setParameter(Integration integration, String value) {
    if (Objects.isNull(integration.getParams())) {
      integration.setParams(new IntegrationParams(new HashMap<>()));
    }
    if (Objects.isNull(integration.getParams().getParams())) {
      integration.getParams().setParams(new HashMap<>());
    }
    integration.getParams().getParams().put(parameterName, value);
  }

  public void setParameter(UpdateAuthRQ request, Integration integration) {
    getParameter(request).ifPresent(it -> setParameter(integration, it));
  }

  public void removeParameter(Integration integration) {
    ofNullable(integration.getParams()).map(IntegrationParams::getParams)
        .ifPresent(params -> params.remove(parameterName));
  }

  public String getRequiredParameter(Integration integration) {
    Optional<String> parameter = getParameter(integration);
    if (required) {
      if (parameter.isPresent()) {
        return parameter.get();
      } else {
        throw new ReportPortalException(ErrorType.INCORRECT_REQUEST,
            formattedSupplier("'{}' should be present.", parameterName));
      }
    } else {
      throw new ReportPortalException(ErrorType.INCORRECT_REQUEST,
          formattedSupplier("'{}' is not required."));
    }
  }

  public String getRequiredParameter(UpdateAuthRQ request) {
    Optional<String> parameter = getParameter(request);
    if (required) {
      if (parameter.isPresent()) {
        return parameter.get();
      } else {
        throw new ReportPortalException(ErrorType.INCORRECT_REQUEST,
            formattedSupplier("'{}' should be present.", parameterName));
      }
    } else {
      throw new ReportPortalException(ErrorType.INCORRECT_REQUEST,
          formattedSupplier("'{}' is not required."));
    }
  }

  /**
   * Reads and validates the raw value of the administrator role values parameter from a request.
   *
   * @param raw value from the request parameters map, may be {@code null}
   * @return trimmed values, de-duplicated ignoring case; empty if {@code raw} is {@code null} or
   *         an empty list
   * @throws ReportPortalException if the value is not a list of non-blank strings, or the list or
   *                               one of the values is too long
   */
  public static List<String> normalizeAdminRoleValues(Object raw) {
    if (raw == null) {
      return List.of();
    }
    if (!(raw instanceof Collection<?> collection)) {
      throw invalidAdminRoleValues("should be a list of strings");
    }
    if (collection.size() > MAX_ADMIN_ROLE_VALUES) {
      throw invalidAdminRoleValues("should not contain more than " + MAX_ADMIN_ROLE_VALUES
          + " values");
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Object item : collection) {
      if (!(item instanceof String value) || StringUtils.isBlank(value)) {
        throw invalidAdminRoleValues("should not contain blank or non-string values");
      }
      var trimmed = value.trim();
      if (trimmed.length() > MAX_ADMIN_ROLE_VALUE_LENGTH) {
        throw invalidAdminRoleValues("values should not be longer than "
            + MAX_ADMIN_ROLE_VALUE_LENGTH + " characters");
      }
      result.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
    }
    return List.copyOf(result.values());
  }

  private static ReportPortalException invalidAdminRoleValues(String reason) {
    return new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
        "'" + ADMIN_ROLE_VALUES.parameterName + "' " + reason);
  }

  /**
   * Returns the stored administrator role values of the integration.
   *
   * @param integration SAML integration
   * @return configured values, or the single default value {@code ADMINISTRATOR} if none are set
   */
  public static List<String> getAdminRoleValues(Integration integration) {
    var params = ofNullable(integration.getParams()).map(IntegrationParams::getParams);
    var configured = params.map(it -> it.get(ADMIN_ROLE_VALUES.parameterName))
        .filter(Collection.class::isInstance)
        .map(it -> (Collection<?>) it).orElseGet(List::of)
        .stream()
        .filter(String.class::isInstance)
        .map(it -> ((String) it).trim())
        .filter(StringUtils::isNotEmpty)
        .toList();
    return configured.isEmpty() ? List.of(DEFAULT_ADMIN_ROLE_VALUE) : configured;
  }

  /**
   * Stores the administrator role values from the request into the integration. A missing
   * parameter leaves the stored value unchanged; an empty list removes it (restoring the default).
   *
   * @param request     update request
   * @param integration integration to update
   */
  public static void setAdminRoleValues(UpdateAuthRQ request, Integration integration) {
    if (request.getIntegrationParams() == null
        || !request.getIntegrationParams().containsKey(ADMIN_ROLE_VALUES.parameterName)) {
      return;
    }
    var values = normalizeAdminRoleValues(
        request.getIntegrationParams().get(ADMIN_ROLE_VALUES.parameterName));
    if (values.isEmpty()) {
      ADMIN_ROLE_VALUES.removeParameter(integration);
    } else {
      if (Objects.isNull(integration.getParams())) {
        integration.setParams(new IntegrationParams(new HashMap<>()));
      }
      if (Objects.isNull(integration.getParams().getParams())) {
        integration.getParams().setParams(new HashMap<>());
      }
      integration.getParams().getParams().put(ADMIN_ROLE_VALUES.parameterName, values);
    }
  }

  public boolean exist(Integration integration) {
    return getParameter(integration).isPresent();
  }

}
