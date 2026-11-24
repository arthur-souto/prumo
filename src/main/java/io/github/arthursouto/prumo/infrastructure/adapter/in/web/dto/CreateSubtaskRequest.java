package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "A new subtask. With createBranch the branch is made in the repository linked to the story")
public record CreateSubtaskRequest(
        @NotBlank @Size(max = 300) String title,
        @Schema(description = "Markdown") @Size(max = 20000) String description,
        @Schema(description = "Defaults to false") Boolean createBranch,
        @Schema(description = "Required with createBranch", example = "feature/PRU-13-write-the-endpoint")
        @Size(max = 200)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*[A-Za-z0-9]$", message = "must be a valid branch name")
        String branchName
) {
}
