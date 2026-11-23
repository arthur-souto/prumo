package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class LinkTokenAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String LINK_TOKEN_PARAMETER = "link_token";
    public static final String LINK_TOKEN_SESSION_ATTRIBUTE = "prumo.oauth.link_token";

    private final OAuth2AuthorizationRequestResolver delegate;

    public LinkTokenAuthorizationRequestResolver(ClientRegistrationRepository registrations, String baseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, baseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return remember(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return remember(request, delegate.resolve(request, clientRegistrationId));
    }

    private static OAuth2AuthorizationRequest remember(HttpServletRequest request, OAuth2AuthorizationRequest resolved) {
        if (resolved == null) {
            return null;
        }

        String linkToken = request.getParameter(LINK_TOKEN_PARAMETER);

        if (linkToken == null || linkToken.isBlank()) {
            request.getSession().removeAttribute(LINK_TOKEN_SESSION_ATTRIBUTE);
        } else {
            request.getSession().setAttribute(LINK_TOKEN_SESSION_ATTRIBUTE, linkToken);
        }

        return resolved;
    }
}
