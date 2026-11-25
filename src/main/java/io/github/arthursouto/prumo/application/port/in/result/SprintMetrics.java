package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

import java.util.List;

public record SprintMetrics(Sprint sprint, int baselinePoints, List<BurndownDay> burndown) {
}
