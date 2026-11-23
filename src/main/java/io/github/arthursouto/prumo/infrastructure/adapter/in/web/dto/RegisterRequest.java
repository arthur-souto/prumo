package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to create a local account")
public record RegisterRequest(
        @Schema(example = "Arthur Souto")
        @NotBlank @Size(max = 150) String name,
        @Schema(example = "arthur@prumo.io")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "s3cret-pass", minLength = 8, maxLength = 72)
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
