package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

public record SprintView(Sprint sprint, int storyCount, int totalPoints, int donePoints) {
}
