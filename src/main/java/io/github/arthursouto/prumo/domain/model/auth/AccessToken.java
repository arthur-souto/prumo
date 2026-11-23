package io.github.arthursouto.prumo.domain.model.auth;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
