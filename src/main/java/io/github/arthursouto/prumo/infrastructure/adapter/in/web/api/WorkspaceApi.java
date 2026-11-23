package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateWorkspaceRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateWorkspaceRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Workspaces", description = "Workspaces the authenticated user belongs to")
@RequestMapping("/api/v1/workspaces")
public interface WorkspaceApi {

    @Operation(
            summary = "Create a workspace",
            description = "The creator becomes its owner. The slug is generated from the name.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Workspace created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
    })
    @PostMapping
    ResponseEntity<WorkspaceResponse> create(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody CreateWorkspaceRequest request
    );

    @Operation(
            summary = "List my workspaces",
            description = "Workspaces where the user is a member, newest first.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspaces of the user"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
    })
    @GetMapping
    List<WorkspaceResponse> list(@Parameter(hidden = true) Authentication authentication);

    @Operation(
            summary = "Get a workspace",
            description = "Only members can see a workspace; others get 404.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Workspace not found or the user is not a member",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/{id}")
    WorkspaceResponse get(@Parameter(hidden = true) Authentication authentication, @PathVariable UUID id);

    @Operation(
            summary = "Update a workspace",
            description = "Owners and admins can change the name and description. The slug does not change.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token"),
            @ApiResponse(responseCode = "403", description = "The member is not an owner or admin"),
            @ApiResponse(responseCode = "404", description = "Workspace not found or the user is not a member")
    })
    @PutMapping("/{id}")
    WorkspaceResponse update(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request
    );

    @Operation(
            summary = "Delete a workspace",
            description = "Only the owner can delete a workspace, with all its memberships.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Workspace deleted"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token"),
            @ApiResponse(responseCode = "403", description = "The member is not the owner"),
            @ApiResponse(responseCode = "404", description = "Workspace not found or the user is not a member")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@Parameter(hidden = true) Authentication authentication, @PathVariable UUID id);
}
