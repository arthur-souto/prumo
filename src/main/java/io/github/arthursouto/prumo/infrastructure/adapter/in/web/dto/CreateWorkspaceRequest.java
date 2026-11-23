package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to create a workspace")
public record CreateWorkspaceRequest(
        @Schema(example = "Acme Engineering")
        @NotBlank @Size(max = 100) String name,
        @Schema(example = "Everything our engineering teams build")
        @Size(max = 500) String description
) {
}
