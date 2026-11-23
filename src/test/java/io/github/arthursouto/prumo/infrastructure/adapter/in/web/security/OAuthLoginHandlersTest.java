package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.application.port.in.command.LinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.in.command.StartOAuthLoginCommand;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth.LinkTokenAuthorizationRequestResolver;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.OAuthLoginFailureHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.OAuthLoginSuccessHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginHandlersTest {

    private static final String CALLBACK = "http://localhost:3000/auth/callback";

    @Mock
    private StartOAuthLoginUseCase startOAuthLogin;

    @Mock
    private LinkOAuthIdentityUseCase linkOAuthIdentity;

    private OAuthLoginSuccessHandler successHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        successHandler = new OAuthLoginSuccessHandler(startOAuthLogin, linkOAuthIdentity, CALLBACK);
        session = new MockHttpSession();
        request = new MockHttpServletRequest();
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsToTheFrontEndWithTheOneTimeCode() throws Exception {
        StartOAuthLoginCommand command = new StartOAuthLoginCommand(
                AuthProvider.GITHUB,
                "42",
                "octo@example.com",
                "The Octocat",
                "https://avatars.example/octo.png"
        );
        when(startOAuthLogin.execute(command)).thenReturn("raw-code");

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes("The Octocat")));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?code=raw-code");
        verify(startOAuthLogin).execute(command);
    }

    @Test
    void fallsBackToTheLoginWhenTheProfileHasNoName() throws Exception {
        StartOAuthLoginCommand command = new StartOAuthLoginCommand(
                AuthProvider.GITHUB,
                "42",
                "octo@example.com",
                "octocat",
                "https://avatars.example/octo.png"
        );
        when(startOAuthLogin.execute(command)).thenReturn("raw-code");

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes(null)));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?code=raw-code");
    }

    @Test
    void doesNotKeepASessionOrASecurityContext() throws Exception {
        when(startOAuthLogin.execute(org.mockito.ArgumentMatchers.any(StartOAuthLoginCommand.class)))
                .thenReturn("raw-code");
        SecurityContextHolder.getContext().setAuthentication(token("github", attributes("The Octocat")));

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes("The Octocat")));

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void redirectsWithTheRejectionReason() throws Exception {
        when(startOAuthLogin.execute(org.mockito.ArgumentMatchers.any(StartOAuthLoginCommand.class)))
                .thenThrow(new OAuthLoginException(OAuthLoginException.Reason.EMAIL_ALREADY_REGISTERED));

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes("The Octocat")));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?error=email_already_registered");
    }

    @Test
    void rejectsProvidersOtherThanGithub() throws Exception {
        successHandler.onAuthenticationSuccess(request, response, token("google", attributes("The Octocat")));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?error=unsupported_provider");
        verifyNoInteractions(startOAuthLogin);
    }

    @Test
    void failureHandlerRedirectsWithAGenericError() throws Exception {
        new OAuthLoginFailureHandler(CALLBACK).onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("anything")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?error=oauth_failed");
    }

    private static Map<String, Object> attributes(String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 42);
        attributes.put("login", "octocat");
        attributes.put("name", name);
        attributes.put("email", "octo@example.com");
        attributes.put("avatar_url", "https://avatars.example/octo.png");
        return attributes;
    }

    private static OAuth2AuthenticationToken token(String registrationId, Map<String, Object> attributes) {
        return new OAuth2AuthenticationToken(
                new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"), attributes, "id"),
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                registrationId
        );
    }

    @Test
    void linksTheIdentityWhenTheSessionCarriesALinkToken() throws Exception {
        session.setAttribute(LinkTokenAuthorizationRequestResolver.LINK_TOKEN_SESSION_ATTRIBUTE, "link-tok");

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes("The Octocat")));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?linked=github");
        verify(linkOAuthIdentity).execute(new LinkOAuthIdentityCommand("link-tok", AuthProvider.GITHUB, "42"));
        verifyNoInteractions(startOAuthLogin);
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void redirectsWithTheReasonWhenLinkingIsRejected() throws Exception {
        session.setAttribute(LinkTokenAuthorizationRequestResolver.LINK_TOKEN_SESSION_ATTRIBUTE, "link-tok");
        doThrow(new OAuthLoginException(OAuthLoginException.Reason.IDENTITY_ALREADY_LINKED))
                .when(linkOAuthIdentity).execute(any(LinkOAuthIdentityCommand.class));

        successHandler.onAuthenticationSuccess(request, response, token("github", attributes("The Octocat")));

        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + "?error=identity_already_linked");
    }
}
