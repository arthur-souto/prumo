package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;

public record LinkOAuthIdentityCommand(String linkToken, AuthProvider provider, String providerId) {
}
