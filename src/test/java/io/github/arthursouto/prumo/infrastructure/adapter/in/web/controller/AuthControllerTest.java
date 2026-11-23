package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.exceptions.InvalidRefreshTokenException;
import io.github.arthursouto.prumo.application.exceptions.InvalidAuthorizationCodeException;
import io.github.arthursouto.prumo.application.port.in.ExchangeOAuthCodeUseCase;
import io.github.arthursouto.prumo.application.port.in.RefreshTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RegisterUserUseCase;
import io.github.arthursouto.prumo.application.port.in.RevokeRefreshTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ExchangeOAuthCodeCommand;
import io.github.arthursouto.prumo.application.port.in.command.RefreshTokensCommand;
import io.github.arthursouto.prumo.application.port.in.command.RegisterUserCommand;
import io.github.arthursouto.prumo.application.port.in.command.RevokeRefreshTokenCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RegisterUserUseCase registerUser;

    @Mock
    private RefreshTokensUseCase refreshTokens;

    @Mock
    private RevokeRefreshTokenUseCase revokeRefreshToken;

    @Mock
    private ExchangeOAuthCodeUseCase exchangeOAuthCode;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AuthController controller = new AuthController(
                registerUser,
                refreshTokens,
                revokeRefreshToken,
                exchangeOAuthCode,
                clock
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreatedUserWithoutSensitiveData() throws Exception {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(registerUser.execute(any(RegisterUserCommand.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Arthur","email":"arthur@prumo.io","password":"s3cret-pass"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.name").value("Arthur"))
                .andExpect(jsonPath("$.email").value("arthur@prumo.io"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(registerUser).execute(new RegisterUserCommand("Arthur", "arthur@prumo.io", "s3cret-pass"));
    }

    @Test
    void registerRejectsInvalidBodyWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"nope","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[2].field").value("password"));

        verifyNoInteractions(registerUser);
    }

    @Test
    void registerReturnsConflictWhenEmailIsTaken() throws Exception {
        when(registerUser.execute(any(RegisterUserCommand.class)))
                .thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Arthur","email":"arthur@prumo.io","password":"s3cret-pass"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void refreshReturnsNewTokensWithoutCookies() throws Exception {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(refreshTokens.execute(new RefreshTokensCommand("old-refresh"))).thenReturn(new AuthTokens(
                user,
                new AccessToken("new-jwt", NOW.plusSeconds(900)),
                "new-refresh",
                NOW.plus(Duration.ofDays(7))
        ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"old-refresh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.access_token").value("new-jwt"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.refresh_token").value("new-refresh"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void refreshReturnsUnauthorizedWhenTheTokenIsRejected() throws Exception {
        when(refreshTokens.execute(new RefreshTokensCommand("bad-refresh")))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"bad-refresh"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }

    @Test
    void refreshRejectsABlankOrMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refreshTokens);
    }

    @Test
    void logoutRevokesTheTokenAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"raw-refresh"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(revokeRefreshToken).execute(new RevokeRefreshTokenCommand("raw-refresh"));
    }

    @Test
    void logoutRejectsABlankToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":" "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(revokeRefreshToken);
    }

    @Test
    void exchangeReturnsTokensForAValidCode() throws Exception {
        User user = User.registerExternal("Octo", "octo@example.com", null);
        when(exchangeOAuthCode.execute(new ExchangeOAuthCodeCommand("raw-code"))).thenReturn(new AuthTokens(
                user,
                new AccessToken("jwt", NOW.plusSeconds(900)),
                "refresh",
                NOW.plus(Duration.ofDays(7))
        ));

        mockMvc.perform(post("/api/v1/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"raw-code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.access_token").value("jwt"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void exchangeReturnsUnauthorizedForABadCode() throws Exception {
        when(exchangeOAuthCode.execute(new ExchangeOAuthCodeCommand("bad-code")))
                .thenThrow(new InvalidAuthorizationCodeException());

        mockMvc.perform(post("/api/v1/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"bad-code"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid authorization code"));
    }

    @Test
    void exchangeRejectsABlankCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"));

        verifyNoInteractions(exchangeOAuthCode);
    }
}
