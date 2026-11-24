package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record CreateSubtaskBranchCommand(UUID userId, UUID workspaceId, String key, String branchName) {
}
