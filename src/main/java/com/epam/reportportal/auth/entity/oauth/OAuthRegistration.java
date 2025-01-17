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

package com.epam.reportportal.auth.entity.oauth;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Andrei Varabyeu
 */
@Getter
@Entity
@Table(name = "oauth_registration", schema = "public")
public class OAuthRegistration implements Serializable {

  @Setter
  @Id
  @Column(name = "id")
  private String id;

  @Setter
  @Column(name = "client_id")
  private String clientId;

  @Setter
  @Column(name = "client_secret")
  private String clientSecret;

  @Setter
  @Column(name = "client_auth_method")
  private String clientAuthMethod;

  @Setter
  @Column(name = "auth_grant_type")
  private String authGrantType;

  @Setter
  @Column(name = "redirect_uri_template")
  private String redirectUrlTemplate;

  @Setter
  @Column(name = "authorization_uri")
  private String authorizationUri;

  @Setter
  @Column(name = "token_uri")
  private String tokenUri;

  @Setter
  @Column(name = "user_info_endpoint_uri")
  private String userInfoEndpointUri;

  @Setter
  @Column(name = "user_info_endpoint_name_attr")
  private String userInfoEndpointNameAttribute;

  @Setter
  @Column(name = "jwk_set_uri")
  private String jwkSetUri;

  @Setter
  @Column(name = "client_name")
  private String clientName;

  @OneToMany(mappedBy = "registration", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
      CascadeType.MERGE,
      CascadeType.REMOVE}, orphanRemoval = true)
  private Set<OAuthRegistrationScope> scopes;

  @OneToMany(mappedBy = "registration", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
      CascadeType.MERGE,
      CascadeType.REMOVE}, orphanRemoval = true)
  private Set<OAuthRegistrationRestriction> restrictions;

  public void setScopes(Set<OAuthRegistrationScope> scopes) {
    if (this.scopes == null) {
      this.scopes = scopes;
    } else {
      this.scopes.retainAll(scopes);
      this.scopes.addAll(scopes);
    }
  }

  public void setRestrictions(Set<OAuthRegistrationRestriction> restrictions) {
    if (this.restrictions == null) {
      this.restrictions = restrictions;
    } else {
      this.restrictions.retainAll(restrictions);
      this.restrictions.addAll(restrictions);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuthRegistration that = (OAuthRegistration) o;
    return Objects.equals(id, that.id) && Objects.equals(clientId, that.clientId) && Objects.equals(
        clientSecret, that.clientSecret)
        && Objects.equals(clientAuthMethod, that.clientAuthMethod) && Objects.equals(authGrantType,
        that.authGrantType)
        && Objects.equals(redirectUrlTemplate, that.redirectUrlTemplate) && Objects.equals(
        authorizationUri, that.authorizationUri)
        && Objects.equals(tokenUri, that.tokenUri) && Objects.equals(userInfoEndpointUri,
        that.userInfoEndpointUri)
        && Objects.equals(userInfoEndpointNameAttribute, that.userInfoEndpointNameAttribute)
        && Objects.equals(jwkSetUri,
        that.jwkSetUri
    ) && Objects.equals(clientName, that.clientName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id,
        clientId,
        clientSecret,
        clientAuthMethod,
        authGrantType,
        redirectUrlTemplate,
        authorizationUri,
        tokenUri,
        userInfoEndpointUri,
        userInfoEndpointNameAttribute,
        jwkSetUri,
        clientName
    );
  }
}
