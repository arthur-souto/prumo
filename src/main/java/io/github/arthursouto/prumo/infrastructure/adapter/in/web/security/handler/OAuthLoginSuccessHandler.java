package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.application.port.in.command.LinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.in.command.StartOAuthLoginCommand;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth.LinkTokenAuthorizationRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final StartOAuthLoginUseCase startOAuthLogin;
    private final LinkOAuthIdentityUseCase linkOAuthIdentity;
    private final String frontendCallbackUrl;

    public OAuthLoginSuccessHandler(StartOAuthLoginUseCase startOAuthLogin,
                                    LinkOAuthIdentityUseCase linkOAuthIdentity,
                                    String frontendCallbackUrl) {
        this.startOAuthLogin = startOAuthLogin;
        this.linkOAuthIdentity = linkOAuthIdentity;
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = token.getPrincipal().getAttributes();

        HttpSession session = request.getSession(false);
        String linkToken = session == null
                ? null
                : (String) session.getAttribute(LinkTokenAuthorizationRequestResolver.LINK_TOKEN_SESSION_ATTRIBUTE);

        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }

        String registrationId = token.getAuthorizedClientRegistrationId();
        response.sendRedirect(linkToken == null
                ? redirectTarget(registrationId, attributes)
                : linkTarget(registrationId, attributes, linkToken));
    }

    private String redirectTarget(String registrationId, Map<String, Object> attributes) {
        UriComponentsBuilder target = UriComponentsBuilder.fromUriString(frontendCallbackUrl);

        if (!"github".equals(registrationId)) {
            return target.queryParam("error", "unsupported_provider").build().toUriString();
        }

        try {
            String code = startOAuthLogin.execute(new StartOAuthLoginCommand(
                    AuthProvider.GITHUB,
                    String.valueOf(attributes.get("id")),
                    asString(attributes.get("email")),
                    displayName(attributes),
                    asString(attributes.get("avatar_url"))
            ));

            return target.queryParam("code", code).build().toUriString();
        } catch (OAuthLoginException exception) {
            return target
                    .queryParam("error", exception.getReason().name().toLowerCase(Locale.ROOT))
                    .build()
                    .toUriString();
        }
    }

    private String linkTarget(String registrationId, Map<String, Object> attributes, String linkToken) {
        UriComponentsBuilder target = UriComponentsBuilder.fromUriString(frontendCallbackUrl);

        if (!"github".equals(registrationId)) {
            return target.queryParam("error", "unsupported_provider").build().toUriString();
        }

        try {
            linkOAuthIdentity.execute(new LinkOAuthIdentityCommand(
                    linkToken,
                    AuthProvider.GITHUB,
                    String.valueOf(attributes.get("id"))
            ));

            return target.queryParam("linked", "github").build().toUriString();
        } catch (OAuthLoginException exception) {
            return target
                    .queryParam("error", exception.getReason().name().toLowerCase(Locale.ROOT))
                    .build()
                    .toUriString();
        }
    }

    private static String displayName(Map<String, Object> attributes) {
        String name = asString(attributes.get("name"));

        return name == null || name.isBlank() ? asString(attributes.get("login")) : name;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
