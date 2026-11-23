package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendCallbackUrl;

    public OAuthLoginFailureHandler(String frontendCallbackUrl) {
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("error", "oauth_failed")
                .build()
                .toUriString());
    }
}
