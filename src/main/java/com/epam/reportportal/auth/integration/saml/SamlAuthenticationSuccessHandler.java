package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.AuthenticationSuccessHandler;
import com.epam.reportportal.auth.ReportPortalClient;
import com.epam.reportportal.auth.TokenServicesFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class SamlAuthenticationSuccessHandler extends AuthenticationSuccessHandler {

    public SamlAuthenticationSuccessHandler(Provider<TokenServicesFacade> tokenFacadeProvider,
                                            ApplicationEventPublisher eventPublisher) {
        super(tokenFacadeProvider, eventPublisher);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected OAuth2AccessToken getToken(Authentication authentication) {
        ReportPortalSamlAuthentication samlAuthentication = (ReportPortalSamlAuthentication) authentication;
        return tokenServicesFacade.get()
                .createToken(ReportPortalClient.ui, samlAuthentication.getName(), samlAuthentication, Collections.emptyMap());
    }
}
