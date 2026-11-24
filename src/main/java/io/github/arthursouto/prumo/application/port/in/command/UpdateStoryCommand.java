package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import java.util.List;
import java.util.UUID;

public record UpdateStoryCommand(
        UUID userId,
        UUID workspaceId,
        String key,
        String title,
        String description,
        StoryPriority priority,
        Integer storyPoints,
        List<String> labels,
        List<ChecklistInput> subtasks,
        List<ChecklistInput> acceptanceCriteria
) {
}
