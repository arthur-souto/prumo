package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SprintSearchRequest(
        @Parameter(description = "Contained in the name or goal, ignoring case and accents")
        @Size(max = 100) String q,
        @Parameter(description = "Any of these states; all when omitted")
        List<SprintState> state,
        @Parameter(description = "Zero-based, defaults to 0")
        @Min(0) @Max(10000) Integer page,
        @Parameter(description = "Defaults to 20")
        @Min(1) @Max(100) Integer size
) {
}
