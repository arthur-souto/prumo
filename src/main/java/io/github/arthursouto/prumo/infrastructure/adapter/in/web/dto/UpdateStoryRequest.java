package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Replaces the editable fields of a story")
public record UpdateStoryRequest(
        @NotBlank @Size(max = 200) String title,
        @Schema(description = "Markdown")
        @Size(max = 20000) String description,
        StoryPriority priority,
        @Min(0) @Max(1000) Integer storyPoints,
        @Size(max = 20) List<@NotBlank @Size(max = 40) String> labels,
        @Size(max = 50) List<@Valid ChecklistItemRequest> acceptanceCriteria
) {
}
