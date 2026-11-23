package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;

import java.util.UUID;

public record UnlinkOAuthIdentityCommand(UUID userId, AuthProvider provider) {
}
