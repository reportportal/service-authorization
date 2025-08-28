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

package com.epam.reportportal.auth.config.saml;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.springframework.http.MediaType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates that SAML provider exists for /saml2/authenticate/{registrationId}. Returns HTTP 404 when unknown provider
 * is requested instead of redirecting to /login.
 *
 * @author <a href="mailto:Reingold_Shekhtel@epam.com">Reingold Shekhtel</a>
 */
public class Saml2RegistrationValidationFilter extends OncePerRequestFilter {

  private static final String AUTHENTICATE_PREFIX = "/saml2/authenticate/";

  private final RelyingPartyRegistrationRepository relyingPartyRepository;

  public Saml2RegistrationValidationFilter(RelyingPartyRegistrationRepository relyingPartyRepository) {
    this.relyingPartyRepository = relyingPartyRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain
  )
      throws ServletException, IOException {

    var servletPath = request.getServletPath();

    if (servletPath != null && servletPath.startsWith(AUTHENTICATE_PREFIX)) {
      var registrationId = servletPath.substring(AUTHENTICATE_PREFIX.length());
      if (!StringUtils.hasText(registrationId) || relyingPartyRepository.findByRegistrationId(registrationId) == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = String.format(
            "{\"code\":\"404\",\"error\":\"SAML provider not found. Check SAML integration.\",\"registrationId\":\"%s\"}",
            registrationId.replace("\\", "\\\\").replace("\"", "\\\"")
        );

        response.getWriter().write(body);

        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}

