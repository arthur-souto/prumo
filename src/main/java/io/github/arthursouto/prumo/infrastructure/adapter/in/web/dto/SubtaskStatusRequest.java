package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import jakarta.validation.constraints.NotNull;

public record SubtaskStatusRequest(@NotNull SubtaskStatus status) {
}
