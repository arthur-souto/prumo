package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "A sprint and the points of its stories")
public record SprintResponse(
        UUID id,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        SprintState state,
        @Schema(description = "Points in the sprint when it started; null until then") Integer committedPoints,
        @Schema(description = "Points done when it closed; null until then") Integer completedPoints,
        int storyCount,
        @Schema(description = "Points of the stories in the sprint now") int totalPoints,
        @Schema(description = "Points of those stories that are in a Done column") int donePoints,
        Instant startedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static SprintResponse from(SprintView view) {
        Sprint sprint = view.sprint();

        return new SprintResponse(
                sprint.getId(),
                sprint.getName(),
                sprint.getGoal(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getState(),
                sprint.getCommittedPoints(),
                sprint.getCompletedPoints(),
                view.storyCount(),
                view.totalPoints(),
                view.donePoints(),
                sprint.getStartedAt(),
                sprint.getClosedAt(),
                sprint.getCreatedAt(),
                sprint.getUpdatedAt()
        );
    }
}
