package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record CreateStoryBranchCommand(UUID userId, UUID workspaceId, String key, String branchName) {
}
