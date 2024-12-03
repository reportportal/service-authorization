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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;

/**
 * Information extracted from SAML response.
 *
 * @author Yevgeniy Svalukhin
 */
public class ReportPortalSamlAuthentication extends Saml2Authentication {

  private static final long serialVersionUID = -289812989450932L;

  private boolean authenticated;
  private List<Attribute> attributes = new LinkedList<>();

  private List<? extends GrantedAuthority> grantedAuthorities;

  private String responseXml;
  private String issuer;

  public ReportPortalSamlAuthentication(Saml2Authentication defaultSamlAuthentication) {
    super((AuthenticatedPrincipal) defaultSamlAuthentication.getPrincipal(),
        defaultSamlAuthentication.getSaml2Response(), Collections.EMPTY_LIST);
    AuthenticatedPrincipal principal = (AuthenticatedPrincipal) defaultSamlAuthentication.getPrincipal();
    this.authenticated = defaultSamlAuthentication.isAuthenticated();
    this.issuer = principal.getName();
  }

  @Override
  public List<Attribute> getDetails() {
    return attributes;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    if (!authenticated && isAuthenticated) {
      throw new IllegalArgumentException(
          "Unable to change state of an existing authentication object.");
    }
  }

  public String getPrincipalName() {
    return getName();
  }

  public String getResponseXml() {
    return responseXml;
  }

  public ReportPortalSamlAuthentication setResponseXml(String responseXml) {
    this.responseXml = responseXml;
    return this;
  }

  public void setAuthorities(List<? extends GrantedAuthority> grantedAuthorities) {
    this.grantedAuthorities = grantedAuthorities;
  }

  public List<? extends GrantedAuthority> getGrantedAuthorities() {
    return grantedAuthorities;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }
}
