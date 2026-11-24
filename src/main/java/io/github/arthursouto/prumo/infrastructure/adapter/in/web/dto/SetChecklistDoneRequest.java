package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record SetChecklistDoneRequest(@NotNull Boolean done) {
}
