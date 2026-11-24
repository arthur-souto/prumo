package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import java.util.UUID;

public record ChangeSubtaskStatusCommand(UUID userId, UUID workspaceId, String key, SubtaskStatus status) {
}
