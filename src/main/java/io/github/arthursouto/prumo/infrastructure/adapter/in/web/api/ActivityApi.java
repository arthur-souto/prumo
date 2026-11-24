package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ActivityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Tag(name = "Activity", description = "Who changed what, and when, on a story and its subtasks")
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public interface ActivityApi {

    @Operation(
            summary = "History of a story",
            description = "Newest first, up to 100 entries, including the ones about its subtasks.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/stories/{key}/activity")
    List<ActivityResponse> ofStory(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );

    @Operation(summary = "History of a subtask", description = "Newest first, up to 100 entries.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/subtasks/{key}/activity")
    List<ActivityResponse> ofSubtask(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable String key
    );
}
