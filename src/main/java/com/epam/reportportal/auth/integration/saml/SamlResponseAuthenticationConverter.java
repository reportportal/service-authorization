/*
 * Copyright 2026 EPAM Systems
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

import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.util.AuthUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Component;

/**
 * Maps a SAML response to a ReportPortal user. It is plugged into
 * {@link OpenSaml4AuthenticationProvider#setResponseAuthenticationConverter(Converter)}, so it is
 * only ever invoked with a response whose signature, conditions, audience, destination and
 * InResponseTo have already been validated by Spring Security. The attributes are taken from the
 * validated (and, if needed, decrypted) assertion as extracted by Spring's default converter; the
 * raw SAMLResponse string is never parsed here.
 */
@Component
public class SamlResponseAuthenticationConverter implements
    Converter<ResponseToken, Saml2Authentication> {

  private final Converter<ResponseToken, Saml2Authentication> defaultConverter =
      OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();

  private final SamlUserReplicator samlUserReplicator;

  public SamlResponseAuthenticationConverter(SamlUserReplicator samlUserReplicator) {
    this.samlUserReplicator = samlUserReplicator;
  }

  @Override
  public Saml2Authentication convert(ResponseToken responseToken) {
    Saml2Authentication validated = defaultConverter.convert(responseToken);
    Saml2AuthenticatedPrincipal validatedPrincipal =
        (Saml2AuthenticatedPrincipal) validated.getPrincipal();

    String registrationId =
        responseToken.getToken().getRelyingPartyRegistration().getRegistrationId();
    Map<String, List<Object>> attributes = validatedPrincipal.getAttributes();

    User user = samlUserReplicator.replicateUser(registrationId, attributes);

    DefaultSaml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal(
        user.getLogin(), Collections.emptyMap(), validatedPrincipal.getSessionIndexes());
    principal.setRelyingPartyRegistrationId(registrationId);

    return new Saml2Authentication(principal, validated.getSaml2Response(),
        AuthUtils.AS_AUTHORITIES.apply(user.getRole()));
  }
}
