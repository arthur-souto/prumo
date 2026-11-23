package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security;

import io.github.arthursouto.prumo.application.exceptions.InvalidCredentialsException;
import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.filter.JsonLoginFilter;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.LoginFailureHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.LoginSuccessHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.provider.LocalAuthenticationProvider;
import io.github.arthursouto.prumo.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JsonLoginFilterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AuthenticateUserUseCase authenticateUser;

    @Mock
    private IssueTokensUseCase issueTokens;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtProperties properties = new JwtProperties(
                "prumo",
                null,
                null,
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        JsonLoginFilter filter = new JsonLoginFilter(
                new ProviderManager(new LocalAuthenticationProvider(authenticateUser)),
                jsonMapper,
                new LoginSuccessHandler(jsonMapper, issueTokens, clock),
                new LoginFailureHandler(jsonMapper)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new Object()).addFilters(filter).build();
    }

    @Test
    void validCredentialsReturnOnlyTheTokens() throws Exception {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(authenticateUser.execute(new AuthenticateUserCommand("arthur@prumo.io", "s3cret-pass")))
                .thenReturn(user);
        when(issueTokens.execute(new IssueTokensCommand(user.getId()))).thenReturn(new AuthTokens(
                user,
                new AccessToken("jwt-value", NOW.plusSeconds(900)),
                "raw-refresh",
                NOW.plus(Duration.ofDays(7))
        ));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"arthur@prumo.io","password":"s3cret-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").value("jwt-value"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.refresh_token").value("raw-refresh"))
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void invalidCredentialsReturnUnauthorizedProblemWithoutTokens() throws Exception {
        when(authenticateUser.execute(new AuthenticateUserCommand("arthur@prumo.io", "wrong-pass")))
                .thenThrow(new InvalidCredentialsException());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"arthur@prumo.io","password":"wrong-pass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("access_token", "refresh_token");
        verifyNoInteractions(issueTokens);
    }

    @Test
    void blankFieldsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid login request"));

        verifyNoInteractions(authenticateUser, issueTokens);
    }

    @Test
    void missingFieldsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authenticateUser, issueTokens);
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid login request"));

        verifyNoInteractions(authenticateUser, issueTokens);
    }

    @Test
    void ignoresRequestsToOtherPathsAndMethods() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/other")).andExpect(status().isNotFound());

        verifyNoInteractions(authenticateUser, issueTokens);
    }
}
