package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record CreateSubtaskCommand(UUID userId, UUID workspaceId, String storyKey, String title, String description, boolean createBranch, String branchName) {
}
