/*
 * Copyright 2025 EPAM Systems
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
package com.epam.reportportal.auth.config.password;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class CustomCodeGrantAuthenticationToken extends
    OAuth2AuthorizationGrantAuthenticationToken {

  private static final long serialVersionUID = 2864586823283322623L;
  @Getter
  private final String username;
  @Getter
  private final String password;
  private final String scope;

  protected CustomCodeGrantAuthenticationToken(
      String granttype,
      Authentication clientPrincipal,
      Map<String, Object> additionalParameters) {
    super(new AuthorizationGrantType(granttype), clientPrincipal, additionalParameters);
    this.username = (String) additionalParameters.get(OAuth2ParameterNames.USERNAME);
    this.password = (String) additionalParameters.get(OAuth2ParameterNames.PASSWORD);
    this.scope = (String) additionalParameters.get(OAuth2ParameterNames.SCOPE);
  }

  public Set<String> getScope() {
    if (scope != null) {
      return StringUtils.commaDelimitedListToSet(scope.replace(" ", ""));
    }
    return Collections.emptySet();
  }

}
