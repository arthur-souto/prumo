package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.application.port.out.query.SortDirection;
import io.github.arthursouto.prumo.application.port.out.query.StorySort;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;

import java.util.List;
import java.util.UUID;

public record ListStoriesQuery(
        UUID userId,
        UUID workspaceId,
        String text,
        List<String> labels,
        List<StoryPriority> priorities,
        List<UUID> columnIds,
        List<UUID> assigneeIds,
        Boolean unassigned,
        StorySort sort,
        SortDirection direction,
        Integer page,
        Integer size
) {
}
