package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record CreateWorkspaceCommand(UUID ownerId, String name, String description) {
}
