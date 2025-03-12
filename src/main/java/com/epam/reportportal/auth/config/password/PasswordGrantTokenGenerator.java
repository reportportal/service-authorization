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

import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import java.util.Collections;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
public class PasswordGrantTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {
  private final TokenServicesFacade tokenService;

  public PasswordGrantTokenGenerator(TokenServicesFacade tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public OAuth2Token generate(OAuth2TokenContext context) {
    String userName = context.getPrincipal().getName();
    return tokenService.createToken(ReportPortalClient.ui.name(), userName, context.getPrincipal().getAuthorities(), Collections.emptyMap());
  }


}
