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

package com.epam.reportportal.auth;

import com.epam.reportportal.auth.event.UiUserSignedInEvent;
import com.epam.reportportal.auth.util.CertificationUtil;
import java.io.IOException;
import java.net.URI;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base class for handling of success authentication and redirection to UI page.
 *
 * @author Yevgeniy Svalukhin
 */
public abstract class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthSuccessHandler.class);

  protected Provider<TokenServicesFacade> tokenServicesFacade;

  private ApplicationEventPublisher eventPublisher;

  @Value("${server.servlet.context-path:/uat}")
  private String pathValue;

  public AuthSuccessHandler(Provider<TokenServicesFacade> tokenServicesFacade,
      ApplicationEventPublisher eventPublisher) {
    super("/");
    this.tokenServicesFacade = tokenServicesFacade;
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected void handle(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    OAuth2AccessToken token = getToken(authentication);

    MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("token", token.getValue());
    query.add("token_type", token.getTokenType());
    LOGGER.debug("Request: " + request);
    LOGGER.debug("pathValue: " + pathValue);
    URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
        .replacePath("/reportportal/ui/authSuccess")
        .replaceQueryParams(query)
        .build()
        .toUri();

    eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

    LOGGER.debug("rqUrl: " + rqUrl.toString());

    getRedirectStrategy().sendRedirect(request, response,
        rqUrl.toString().replaceFirst("authSuccess", "#authSuccess").replaceFirst("/ui", "/reportportal/ui"));
  }

  protected abstract OAuth2AccessToken getToken(Authentication authentication);
}
