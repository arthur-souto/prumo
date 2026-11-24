package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateBranchRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateSubtaskRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SubtaskResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SubtaskStatusRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateSubtaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Subtasks", description = "Subtasks of a user story. A branch belongs to a subtask")
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public interface SubtaskApi {

    @Operation(
            summary = "Create a subtask",
            description = "The key continues the workspace sequence. With createBranch the branch is created on GitHub "
                    + "first; GitHub failures come back with a code property.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/stories/{storyKey}/subtasks")
    ResponseEntity<SubtaskResponse> create(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String storyKey,
            @Valid @RequestBody CreateSubtaskRequest request
    );

    @Operation(summary = "List the subtasks of a story", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/stories/{storyKey}/subtasks")
    List<SubtaskResponse> list(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String storyKey
    );

    @Operation(summary = "Get a subtask by key", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/subtasks/{key}")
    SubtaskResponse get(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Edit title and description", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/subtasks/{key}")
    SubtaskResponse update(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody UpdateSubtaskRequest request
    );

    @Operation(
            summary = "Change the status",
            description = "When the board consolidates stories, the last subtask turning DONE moves the story to Done.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/subtasks/{key}/status")
    SubtaskResponse changeStatus(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody SubtaskStatusRequest request
    );

    @Operation(summary = "Delete a subtask", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/subtasks/{key}")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Suggest a branch name", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/subtasks/{key}/branch-suggestion")
    Map<String, String> suggestBranch(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(
            summary = "Create the branch of a subtask",
            description = "Uses the repository linked to the story. A subtask has at most one branch.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/subtasks/{key}/branch")
    SubtaskResponse createBranch(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody CreateBranchRequest request
    );
}
