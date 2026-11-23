package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New name and description of a workspace")
public record UpdateWorkspaceRequest(
        @Schema(example = "Acme Platform")
        @NotBlank @Size(max = 100) String name,
        @Schema(example = "Platform and infrastructure teams")
        @Size(max = 500) String description
) {
}
