package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Instant;

public record AuthTokens(User user, AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
