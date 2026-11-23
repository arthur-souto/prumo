package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials of a local account")
public record LoginRequest(
        @Schema(example = "arthur@prumo.io")
        @NotBlank String email,
        @Schema(example = "s3cret-pass")
        @NotBlank String password
) {
}
