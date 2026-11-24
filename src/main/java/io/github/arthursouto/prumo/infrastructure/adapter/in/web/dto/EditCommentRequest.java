package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "The new text of a comment")
public record EditCommentRequest(
        @Schema(description = "Markdown")
        @NotBlank @Size(max = 20000) String body
) {
}
