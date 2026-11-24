package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Board settings. Omitted fields stay as they are")
public record UpdateBoardSettingsRequest(
        @Schema(description = "Only changeable while the workspace has no stories", example = "PRU")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{1,9}$") String keyPrefix,
        @Schema(description = "A pull request moves its subtask: opened means IN_REVIEW, merged means DONE")
        Boolean automationEnabled,
        @Schema(description = "Move the story to the Done column when all of its subtasks are DONE")
        Boolean consolidateStories
) {
}
