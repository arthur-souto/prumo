package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.result.SprintMetrics;

public interface GetSprintMetricsUseCase {

    SprintMetrics execute(GetSprintQuery query);
}
