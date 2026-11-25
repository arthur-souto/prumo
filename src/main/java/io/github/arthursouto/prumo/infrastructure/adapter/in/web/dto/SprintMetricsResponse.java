package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.BurndownDay;
import io.github.arthursouto.prumo.application.port.in.result.SprintMetrics;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "The burndown of a sprint, one entry per day from its start to its end")
public record SprintMetricsResponse(
        UUID sprintId,
        SprintState state,
        LocalDate startDate,
        LocalDate endDate,
        Integer committedPoints,
        Integer completedPoints,
        @Schema(description = "Where the ideal line starts: the committed points, or the first recorded total when the sprint started empty") int baselinePoints,
        List<Day> burndown
) {

    @Schema(description = "A day of the sprint. The recorded values are null for days still to come and days with nothing recorded yet")
    public record Day(
            LocalDate date,
            Integer totalPoints,
            Integer remainingPoints,
            Integer totalStories,
            Integer remainingStories,
            @Schema(description = "Points left on the straight line from the baseline to zero on the last day") double idealPoints
    ) {

        static Day from(BurndownDay day) {
            return new Day(day.date(), day.totalPoints(), day.remainingPoints(), day.totalStories(), day.remainingStories(), day.idealPoints());
        }
    }

    public static SprintMetricsResponse from(SprintMetrics metrics) {
        Sprint sprint = metrics.sprint();

        return new SprintMetricsResponse(
                sprint.getId(),
                sprint.getState(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getCommittedPoints(),
                sprint.getCompletedPoints(),
                metrics.baselinePoints(),
                metrics.burndown().stream().map(Day::from).toList()
        );
    }
}
