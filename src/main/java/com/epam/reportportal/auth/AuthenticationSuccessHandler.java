package com.epam.reportportal.auth;

import com.epam.reportportal.auth.event.UiUserSignedInEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

public abstract class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    protected Provider<TokenServicesFacade> tokenServicesFacade;

    protected ApplicationEventPublisher eventPublisher;

    public AuthenticationSuccessHandler(Provider<TokenServicesFacade> tokenFacadeProvider,
                                        ApplicationEventPublisher eventPublisher) {
        super("/");
        this.tokenServicesFacade = tokenFacadeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2AccessToken accessToken = getToken(authentication);

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("token", accessToken.getValue());
        query.add("token_type", accessToken.getTokenType());
        URI rqUrl = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).replacePath("/ui/authSuccess.html")
                .replaceQueryParams(query).build().toUri();

        eventPublisher.publishEvent(new UiUserSignedInEvent(authentication));

        getRedirectStrategy().sendRedirect(request, response, rqUrl.toString());
    }

    protected abstract OAuth2AccessToken getToken(Authentication authentication);
}
