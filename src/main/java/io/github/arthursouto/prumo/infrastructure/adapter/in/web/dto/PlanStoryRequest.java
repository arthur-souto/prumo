package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "The sprint of the story; null sends it back to the backlog")
public record PlanStoryRequest(UUID sprintId) {
}
