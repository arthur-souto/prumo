package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "The name, goal and dates of a sprint")
public record SprintRequest(
        @Schema(example = "Sprint 7")
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String goal,
        @Schema(example = "2026-12-01")
        @NotNull LocalDate startDate,
        @Schema(example = "2026-12-14")
        @NotNull LocalDate endDate
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "must not be before the start date")
    public boolean isEndOnOrAfterStart() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
