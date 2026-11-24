package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A new comment")
public record AddCommentRequest(
        @Schema(description = "Markdown", example = "We decided to keep the rank column, see the thread above")
        @NotBlank @Size(max = 20000) String body
) {
}
