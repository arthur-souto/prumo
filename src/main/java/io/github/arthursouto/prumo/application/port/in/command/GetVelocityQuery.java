package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record GetVelocityQuery(UUID userId, UUID workspaceId, Integer limit) {
}
