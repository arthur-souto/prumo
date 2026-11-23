package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record GetUserIdentitiesQuery(UUID userId) {
}
