package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.BoardResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ColumnNameRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ReorderColumnsRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateBoardSettingsRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Board", description = "Kanban board of a workspace")
@RequestMapping("/api/v1/workspaces/{workspaceId}/board")
public interface BoardApi {

    @Operation(
            summary = "Get the board",
            description = "Columns and stories. The board is created with To Do, In Progress, In Review and Done on first access.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The board"),
            @ApiResponse(responseCode = "404", description = "Workspace not found or the user is not a member")
    })
    @GetMapping
    BoardResponse get(@Parameter(hidden = true) Authentication authentication, @PathVariable UUID workspaceId);

    @Operation(
            summary = "Update board settings",
            description = "Owners and admins. The key prefix is locked once the workspace has stories.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings updated"),
            @ApiResponse(responseCode = "403", description = "The member is not an owner or admin"),
            @ApiResponse(responseCode = "409", description = "The key prefix is in use"),
            @ApiResponse(responseCode = "422", description = "The key prefix cannot change anymore")
    })
    @PutMapping("/settings")
    BoardResponse updateSettings(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateBoardSettingsRequest request
    );

    @Operation(summary = "Create a column", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Column created"),
            @ApiResponse(responseCode = "409", description = "A column with this name exists")
    })
    @PostMapping("/columns")
    ResponseEntity<BoardResponse.ColumnResponse> createColumn(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ColumnNameRequest request
    );

    @Operation(summary = "Rename a column", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/columns/{columnId}")
    BoardResponse.ColumnResponse renameColumn(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID columnId,
            @Valid @RequestBody ColumnNameRequest request
    );

    @Operation(
            summary = "Reorder the columns",
            description = "Send every column id in the new order.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/columns/order")
    BoardResponse reorderColumns(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ReorderColumnsRequest request
    );

    @Operation(
            summary = "Remove a column",
            description = "Only an empty column, unless moveTo names the column that receives its stories.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Column removed"),
            @ApiResponse(responseCode = "422", description = "The column has stories and no destination, or it is the last column")
    })
    @DeleteMapping("/columns/{columnId}")
    ResponseEntity<Void> deleteColumn(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID columnId,
            @RequestParam(required = false) UUID moveTo
    );
}
