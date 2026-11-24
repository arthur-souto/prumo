package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Replaces the editable fields of a subtask")
public record UpdateSubtaskRequest(
        @NotBlank @Size(max = 300) String title,
        @Schema(description = "Markdown") @Size(max = 20000) String description
) {
}
