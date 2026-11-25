package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.PageResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.PlanStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintSearchRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.StoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Sprints", description = "Sprints of a workspace. Owners and admins manage them; any member plans stories into them")
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public interface SprintApi {

    @Operation(summary = "Search the sprints, a page at a time",
            description = "The active one first, then the planned ones by start date, then the closed ones, the latest first.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/sprints")
    PageResponse<SprintResponse> list(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @ParameterObject SprintSearchRequest search
    );

    @Operation(summary = "Get a sprint", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/sprints/{sprintId}")
    SprintResponse get(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID sprintId
    );

    @Operation(summary = "Create a planned sprint", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sprint created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Only owners and admins manage sprints")
    })
    @PostMapping("/sprints")
    ResponseEntity<SprintResponse> create(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SprintRequest request
    );

    @Operation(summary = "Change the name, goal or dates", description = "A closed sprint cannot be changed (422).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/sprints/{sprintId}")
    SprintResponse update(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID sprintId,
            @Valid @RequestBody SprintRequest request
    );

    @Operation(summary = "Start a planned sprint", description = "Records the committed points. Only one sprint is active at a time.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sprint started"),
            @ApiResponse(responseCode = "409", description = "Another sprint is already active"),
            @ApiResponse(responseCode = "422", description = "The sprint is not planned")
    })
    @PostMapping("/sprints/{sprintId}/start")
    SprintResponse start(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID sprintId
    );

    @Operation(summary = "Close the active sprint",
            description = "Records the completed points, the ones of stories in a Done column. Unfinished stories go back to the backlog.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sprint closed"),
            @ApiResponse(responseCode = "422", description = "The sprint is not active")
    })
    @PostMapping("/sprints/{sprintId}/close")
    SprintResponse close(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID sprintId
    );

    @Operation(summary = "Delete a planned sprint", description = "Its stories go back to the backlog.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/sprints/{sprintId}")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID sprintId
    );

    @Operation(summary = "Plan a story into a sprint, or send it back to the backlog",
            description = "A closed sprint takes no stories (422).", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/stories/{key}/sprint")
    StoryResponse plan(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key,
            @RequestBody PlanStoryRequest request
    );
}
