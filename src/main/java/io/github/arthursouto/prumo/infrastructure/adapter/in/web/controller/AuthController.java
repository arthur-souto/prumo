package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ExchangeOAuthCodeUseCase;
import io.github.arthursouto.prumo.application.port.in.RefreshTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RegisterUserUseCase;
import io.github.arthursouto.prumo.application.port.in.RevokeRefreshTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ExchangeOAuthCodeCommand;
import io.github.arthursouto.prumo.application.port.in.command.RefreshTokensCommand;
import io.github.arthursouto.prumo.application.port.in.command.RegisterUserCommand;
import io.github.arthursouto.prumo.application.port.in.command.RevokeRefreshTokenCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.AuthApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ExchangeCodeRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.RefreshTokenRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.RegisterRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.TokenResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UserResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
public class AuthController implements AuthApi {

    private final RegisterUserUseCase registerUser;
    private final RefreshTokensUseCase refreshTokens;
    private final RevokeRefreshTokenUseCase revokeRefreshToken;
    private final ExchangeOAuthCodeUseCase exchangeOAuthCode;
    private final Clock clock;

    public AuthController(RegisterUserUseCase registerUser,
                          RefreshTokensUseCase refreshTokens,
                          RevokeRefreshTokenUseCase revokeRefreshToken,
                          ExchangeOAuthCodeUseCase exchangeOAuthCode,
                          Clock clock) {
        this.registerUser = registerUser;
        this.refreshTokens = refreshTokens;
        this.revokeRefreshToken = revokeRefreshToken;
        this.exchangeOAuthCode = exchangeOAuthCode;
        this.clock = clock;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        User user = registerUser.execute(new RegisterUserCommand(
                request.name(),
                request.email(),
                request.password()
        ));

        return UserResponse.from(user);
    }

    @Override
    public ResponseEntity<TokenResponse> refresh(RefreshTokenRequest request) {
        AuthTokens tokens = refreshTokens.execute(new RefreshTokensCommand(request.refreshToken()));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TokenResponse.from(tokens, clock.instant()));
    }

    @Override
    public ResponseEntity<TokenResponse> exchangeOAuthCode(ExchangeCodeRequest request) {
        AuthTokens tokens = exchangeOAuthCode.execute(new ExchangeOAuthCodeCommand(request.code()));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TokenResponse.from(tokens, clock.instant()));
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        revokeRefreshToken.execute(new RevokeRefreshTokenCommand(request.refreshToken()));
    }
}
