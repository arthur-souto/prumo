package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;

public record StartOAuthLoginCommand(
        AuthProvider provider,
        String providerId,
        String email,
        String name,
        String avatarUrl
) {
}
