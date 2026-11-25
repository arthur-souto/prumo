package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

public record VelocitySprint(Sprint sprint, Integer committedPoints) {
}
