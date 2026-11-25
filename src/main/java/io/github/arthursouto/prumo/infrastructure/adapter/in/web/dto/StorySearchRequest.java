package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.out.query.SortDirection;
import io.github.arthursouto.prumo.application.port.out.query.StorySort;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record StorySearchRequest(
        @Parameter(description = "Contained in the title, key or description, ignoring case")
        @Size(max = 100) String q,
        @Parameter(description = "Any of these labels, ignoring case")
        @Size(max = 20) List<@NotBlank @Size(max = 40) String> label,
        @Parameter(description = "Any of these priorities")
        List<StoryPriority> priority,
        @Parameter(description = "In any of these columns")
        @Size(max = 50) List<UUID> columnId,
        @Parameter(description = "Assigned to any of these members")
        @Size(max = 50) List<UUID> assigneeId,
        @Parameter(description = "Include unassigned stories; combined with assigneeId as an OR")
        Boolean unassigned,
        @Parameter(description = "In any of these sprints")
        @Size(max = 50) List<UUID> sprintId,
        @Parameter(description = "Include stories in no sprint (the backlog); combined with sprintId as an OR")
        Boolean noSprint,
        @Parameter(description = "Defaults to NUMBER, the newest first")
        StorySort sort,
        @Parameter(description = "Defaults to DESC")
        SortDirection direction,
        @Parameter(description = "Zero-based, defaults to 0")
        @Min(0) @Max(10000) Integer page,
        @Parameter(description = "Defaults to 20")
        @Min(1) @Max(100) Integer size
) {
}
