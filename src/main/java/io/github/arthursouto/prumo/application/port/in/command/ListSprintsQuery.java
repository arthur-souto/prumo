package io.github.arthursouto.prumo.application.port.in.command;

import io.github.arthursouto.prumo.domain.model.sprint.SprintState;

import java.util.List;
import java.util.UUID;

public record ListSprintsQuery(UUID userId, UUID workspaceId, String q, List<SprintState> states, Integer page, Integer size) {
}
