package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.AddCommentRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CommentResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.EditCommentRequest;
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
import java.util.UUID;

@Tag(name = "Comments", description = "Markdown comments on stories and subtasks. Only the author edits or deletes a comment")
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public interface CommentApi {

    @Operation(summary = "List the comments of a story", description = "Oldest first. Comments on its subtasks are not included.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/stories/{key}/comments")
    List<CommentResponse> ofStory(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Comment on a story", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/stories/{key}/comments")
    ResponseEntity<CommentResponse> addToStory(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody AddCommentRequest request
    );

    @Operation(summary = "List the comments of a subtask", description = "Oldest first.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/subtasks/{key}/comments")
    List<CommentResponse> ofSubtask(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "Comment on a subtask", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/subtasks/{key}/comments")
    ResponseEntity<CommentResponse> addToSubtask(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @Valid @RequestBody AddCommentRequest request
    );

    @Operation(summary = "Edit a comment", description = "Only its author can; anyone else gets 403.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/comments/{commentId}")
    CommentResponse edit(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID commentId,
            @Valid @RequestBody EditCommentRequest request
    );

    @Operation(summary = "Delete a comment", description = "Only its author can; anyone else gets 403.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/comments/{commentId}")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID commentId
    );
}
