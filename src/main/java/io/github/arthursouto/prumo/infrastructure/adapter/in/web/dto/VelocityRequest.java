package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record VelocityRequest(
        @Schema(description = "How many of the latest closed sprints; 6 by default") @Min(1) @Max(20) Integer limit
) {
}
