package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

import java.util.List;

public record Velocity(List<Sprint> sprints, double averageCompleted, double averageCommitted) {
}
