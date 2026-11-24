package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import java.util.UUID;

public record ListStoriesQuery(UUID userId, UUID workspaceId, String label, StoryPriority priority, String text) {
}
