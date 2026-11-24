package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateBranchRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateStoryRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LinkRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.MoveStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SetChecklistDoneRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.StoryResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateStoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Stories", description = "User stories of a workspace")
@RequestMapping("/api/v1/workspaces/{workspaceId}/stories")
public interface StoryApi {

    @Operation(
            summary = "Create a story",
            description = "The key is the board prefix plus a per-workspace sequence, such as PRU-12.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Story created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Workspace not found or the user is not a member")
    })
    @PostMapping
    ResponseEntity<StoryResponse> create(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateStoryRequest request
    );

    @Operation(summary = "List and search stories", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    List<StoryResponse> list(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) StoryPriority priority,
            @RequestParam(name = "q", required = false) String text
    );

    @Operation(summary = "Get a story by key", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{key}")
    StoryResponse get(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Update a story", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{key}")
    StoryResponse update(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody UpdateStoryRequest request
    );

    @Operation(summary = "Delete a story", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{key}")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Tick or untick a subtask or acceptance criterion", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{key}/checklist/{itemId}")
    StoryResponse setChecklistItemDone(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @PathVariable UUID itemId,
            @Valid @RequestBody SetChecklistDoneRequest request
    );

    @Operation(
            summary = "Move a story",
            description = "Atomic move to a column and a position between two neighbours. Only this story is written.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story moved"),
            @ApiResponse(responseCode = "404", description = "Story or column not found"),
            @ApiResponse(responseCode = "422", description = "The neighbours are not in the destination column")
    })
    @PutMapping("/{key}/move")
    StoryResponse move(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody MoveStoryRequest request
    );

    @Operation(summary = "Link or unlink a connected repository", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{key}/repository")
    StoryResponse linkRepository(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @RequestBody LinkRepositoryRequest request
    );

    @Operation(summary = "Suggest a branch name", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{key}/branch-suggestion")
    Map<String, String> suggestBranch(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(
            summary = "Create a branch in the linked repository",
            description = "GitHub failures come back with a code property: INVALID_TOKEN, RATE_LIMITED, NOT_FOUND and so on.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{key}/branch")
    StoryResponse createBranch(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody CreateBranchRequest request
    );

    @Operation(
            summary = "Create a GitHub repository for the story",
            description = "The repository is connected to the workspace and linked to the story.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{key}/new-repository")
    StoryResponse createRepository(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody CreateStoryRepositoryRequest request
    );
}
