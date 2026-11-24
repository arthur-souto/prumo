package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "A subtask or acceptance criterion. Send the id to keep an existing item")
public record ChecklistItemRequest(
        UUID id,
        @Schema(example = "User can reset the password")
        @NotBlank @Size(max = 300) String text,
        Boolean done
) {
}
