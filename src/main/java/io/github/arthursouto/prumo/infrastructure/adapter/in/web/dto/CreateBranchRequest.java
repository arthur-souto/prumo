package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @Schema(example = "feature/PRU-12-reset-password")
        @NotBlank @Size(max = 200)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*[A-Za-z0-9]$", message = "must be a valid branch name")
        String name
) {
}
