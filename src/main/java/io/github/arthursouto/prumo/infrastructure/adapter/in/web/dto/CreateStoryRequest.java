package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "A new user story. It goes to the first column unless columnId is given")
public record CreateStoryRequest(
        @Schema(example = "As a user I want to reset my password")
        @NotBlank @Size(max = 200) String title,
        @Schema(description = "Markdown")
        @Size(max = 20000) String description,
        StoryPriority priority,
        @Min(0) @Max(1000) Integer storyPoints,
        @Size(max = 20) List<@NotBlank @Size(max = 40) String> labels,
        @Size(max = 50) List<@Valid ChecklistItemRequest> acceptanceCriteria,
        UUID columnId
) {
}
