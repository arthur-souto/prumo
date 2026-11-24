package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectRepositoryRequest(
        @Schema(example = "arthur-souto")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$") String owner,
        @Schema(example = "prumo")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$") String name
) {
}
