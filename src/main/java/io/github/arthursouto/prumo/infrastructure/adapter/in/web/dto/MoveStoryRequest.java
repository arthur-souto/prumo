package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Destination of a card. Give the story that will sit above it, below it, or both")
public record MoveStoryRequest(
        @NotNull UUID columnId,
        UUID afterStoryId,
        UUID beforeStoryId
) {
}
