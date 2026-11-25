package io.github.arthursouto.prumo.application.port.out.query;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;

import java.util.Set;
import java.util.UUID;

public record StorySearchCriteria(
        UUID workspaceId,
        String text,
        Set<String> labels,
        Set<StoryPriority> priorities,
        Set<UUID> columnIds,
        Set<UUID> assigneeIds,
        boolean unassigned,
        Set<UUID> sprintIds,
        boolean withoutSprint,
        StorySort sort,
        SortDirection direction,
        int page,
        int size
) {

    public int offset() {
        return page * size;
    }
}
