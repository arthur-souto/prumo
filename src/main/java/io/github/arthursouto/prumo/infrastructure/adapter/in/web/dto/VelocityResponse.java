package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.Velocity;
import io.github.arthursouto.prumo.application.port.in.result.VelocitySprint;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Committed and completed points of the latest closed sprints, the oldest first")
public record VelocityResponse(
        List<Entry> sprints,
        @Schema(description = "Average completed points of these sprints") double averageCompleted,
        @Schema(description = "Average committed points of these sprints") double averageCommitted
) {

    public record Entry(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            @Schema(description = "Points when it started, or its first recorded total when it started empty") Integer committedPoints,
            Integer completedPoints
    ) {

        static Entry from(VelocitySprint entry) {
            Sprint sprint = entry.sprint();
            return new Entry(sprint.getId(), sprint.getName(), sprint.getStartDate(), sprint.getEndDate(),
                    entry.committedPoints(), sprint.getCompletedPoints());
        }
    }

    public static VelocityResponse from(Velocity velocity) {
        return new VelocityResponse(
                velocity.sprints().stream().map(Entry::from).toList(),
                velocity.averageCompleted(),
                velocity.averageCommitted()
        );
    }
}
