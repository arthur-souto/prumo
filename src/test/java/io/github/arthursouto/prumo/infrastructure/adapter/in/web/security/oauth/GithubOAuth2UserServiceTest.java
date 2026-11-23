package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GithubOAuth2UserServiceTest {

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    private MockRestServiceServer server;
    private GithubOAuth2UserService service;
    private OAuth2UserRequest request;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.test");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GithubOAuth2UserService(delegate, builder.build());

        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://github.test/authorize")
                .tokenUri("https://github.test/token")
                .userInfoUri("https://api.github.test/user")
                .userNameAttributeName("id")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "gho_token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        request = new OAuth2UserRequest(registration, accessToken);

        when(delegate.loadUser(request)).thenReturn(new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                Map.of("id", 42, "login", "octocat", "name", "The Octocat", "email", "public@example.com"),
                "id"
        ));
    }

    @Test
    void usesThePrimaryVerifiedEmailInsteadOfThePublicProfileEmail() {
        expectEmails("""
                [
                  {"email":"secondary@example.com","primary":false,"verified":true},
                  {"email":"unverified@example.com","primary":true,"verified":false},
                  {"email":"primary@example.com","primary":true,"verified":true}
                ]
                """);

        OAuth2User user = service.loadUser(request);

        assertThat(user.<String>getAttribute("email")).isEqualTo("primary@example.com");
        assertThat(user.<String>getAttribute("login")).isEqualTo("octocat");
        assertThat(user.<String>getAttribute("name")).isEqualTo("The Octocat");
        assertThat(user.getName()).isEqualTo("42");
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        server.verify();
    }

    @Test
    void fallsBackToAnotherVerifiedEmailWhenNoneIsPrimary() {
        expectEmails("""
                [
                  {"email":"unverified@example.com","primary":false,"verified":false},
                  {"email":"secondary@example.com","primary":false,"verified":true}
                ]
                """);

        assertThat(service.loadUser(request).<String>getAttribute("email")).isEqualTo("secondary@example.com");
    }

    @Test
    void leavesTheEmailOutWhenNoneIsVerified() {
        expectEmails("""
                [{"email":"unverified@example.com","primary":true,"verified":false}]
                """);

        OAuth2User user = service.loadUser(request);

        assertThat(user.getAttributes()).doesNotContainKey("email");
    }

    @Test
    void failsTheLoginWhenTheEmailsCannotBeFetched() {
        server.expect(requestTo("https://api.github.test/user/emails"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(exception -> ((OAuth2AuthenticationException) exception).getError().getErrorCode())
                .isEqualTo("email_fetch_failed");
    }

    private void expectEmails(String json) {
        server.expect(requestTo("https://api.github.test/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gho_token"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
