package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoryRepositoryRequest(
        @Schema(example = "reset-password-service")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$") String name,
        @Size(max = 350) String description,
        Boolean isPrivate
) {
}
