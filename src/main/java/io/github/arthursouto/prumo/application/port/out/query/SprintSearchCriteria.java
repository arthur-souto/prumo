package io.github.arthursouto.prumo.application.port.out.query;

import io.github.arthursouto.prumo.domain.model.sprint.SprintState;

import java.util.Set;
import java.util.UUID;

public record SprintSearchCriteria(UUID workspaceId, String text, Set<SprintState> states, int page, int size) {
}
