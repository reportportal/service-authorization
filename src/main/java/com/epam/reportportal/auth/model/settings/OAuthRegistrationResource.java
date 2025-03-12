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

package com.epam.reportportal.auth.model.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Settings for OAuth provider registration.
 *
 * @author Anton Machulski
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthRegistrationResource implements Serializable {

  public static final String URL_PATTERN = "^(http://|https://)?(www\\.)?([a-zA-Z0-9-]+)(\\.[a-zA-Z0-9-]+)*(:[0-9]+)?(/[a-z_-]+)*$";

  @JsonProperty(value = "id")
  private String id;

  @NotBlank
  @JsonProperty(value = "clientId")
  private String clientId;

  @NotBlank
  @JsonProperty(value = "clientSecret")
  private String clientSecret;

  @JsonProperty(value = "clientAuthMethod")
  private String clientAuthMethod;

  @JsonProperty(value = "authGrantType")
  private String authGrantType;

  @JsonProperty(value = "redirectUrlTemplate")
  private String redirectUrlTemplate;

  @Pattern(regexp = URL_PATTERN)
  @JsonProperty(value = "authorizationUri")
  @Schema(type = "string", pattern = URL_PATTERN, example = "string")
  private String authorizationUri;

  @Pattern(regexp = URL_PATTERN)
  @JsonProperty(value = "tokenUri")
  @Schema(type = "string", pattern = URL_PATTERN, example = "string")
  private String tokenUri;

  @JsonProperty(value = "userInfoEndpointUri")
  private String userInfoEndpointUri;

  @JsonProperty(value = "userInfoEndpointNameAttribute")
  private String userInfoEndpointNameAttribute;

  @JsonProperty(value = "jwkSetUri")
  private String jwkSetUri;

  @JsonProperty(value = "clientName")
  private String clientName;

  @JsonProperty(value = "scopes")
  private Set<String> scopes;

  @JsonProperty(value = "restrictions")
  private Map<String, String> restrictions;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OAuthRegistrationResource that = (OAuthRegistrationResource) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) {
      return false;
    }
    if (clientSecret != null ? !clientSecret.equals(that.clientSecret)
        : that.clientSecret != null) {
      return false;
    }
    if (clientAuthMethod != null ? !clientAuthMethod.equals(that.clientAuthMethod)
        : that.clientAuthMethod != null) {
      return false;
    }
    if (authGrantType != null ? !authGrantType.equals(that.authGrantType)
        : that.authGrantType != null) {
      return false;
    }
    if (redirectUrlTemplate != null ? !redirectUrlTemplate.equals(that.redirectUrlTemplate)
        : that.redirectUrlTemplate != null) {
      return false;
    }
    if (authorizationUri != null ? !authorizationUri.equals(that.authorizationUri)
        : that.authorizationUri != null) {
      return false;
    }
    if (tokenUri != null ? !tokenUri.equals(that.tokenUri) : that.tokenUri != null) {
      return false;
    }
    if (userInfoEndpointUri != null ? !userInfoEndpointUri.equals(that.userInfoEndpointUri)
        : that.userInfoEndpointUri != null) {
      return false;
    }
    if (userInfoEndpointNameAttribute != null ?
        !userInfoEndpointNameAttribute.equals(that.userInfoEndpointNameAttribute) :
        that.userInfoEndpointNameAttribute != null) {
      return false;
    }
    if (jwkSetUri != null ? !jwkSetUri.equals(that.jwkSetUri) : that.jwkSetUri != null) {
      return false;
    }
    if (clientName != null ? !clientName.equals(that.clientName) : that.clientName != null) {
      return false;
    }
    if (scopes != null ? !scopes.equals(that.scopes) : that.scopes != null) {
      return false;
    }
    return restrictions != null ? restrictions.equals(that.restrictions)
        : that.restrictions == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
    result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
    result = 31 * result + (clientAuthMethod != null ? clientAuthMethod.hashCode() : 0);
    result = 31 * result + (authGrantType != null ? authGrantType.hashCode() : 0);
    result = 31 * result + (redirectUrlTemplate != null ? redirectUrlTemplate.hashCode() : 0);
    result = 31 * result + (authorizationUri != null ? authorizationUri.hashCode() : 0);
    result = 31 * result + (tokenUri != null ? tokenUri.hashCode() : 0);
    result = 31 * result + (userInfoEndpointUri != null ? userInfoEndpointUri.hashCode() : 0);
    result = 31 * result + (userInfoEndpointNameAttribute != null
        ? userInfoEndpointNameAttribute.hashCode() : 0);
    result = 31 * result + (jwkSetUri != null ? jwkSetUri.hashCode() : 0);
    result = 31 * result + (clientName != null ? clientName.hashCode() : 0);
    result = 31 * result + (scopes != null ? scopes.hashCode() : 0);
    result = 31 * result + (restrictions != null ? restrictions.hashCode() : 0);
    return result;
  }
}
