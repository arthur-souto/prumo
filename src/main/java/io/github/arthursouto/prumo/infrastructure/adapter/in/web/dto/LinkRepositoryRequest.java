package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record LinkRepositoryRequest(
        @Schema(description = "A repository connected to the workspace. Null removes the link")
        UUID repositoryId
) {
}
