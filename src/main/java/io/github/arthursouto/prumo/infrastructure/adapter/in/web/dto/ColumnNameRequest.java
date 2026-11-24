package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColumnNameRequest(@NotBlank @Size(max = 60) String name) {
}
