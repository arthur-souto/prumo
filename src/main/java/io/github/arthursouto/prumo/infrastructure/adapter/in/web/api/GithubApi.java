package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ConnectRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.GithubConnectionResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.RepositoryResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SaveGithubTokenRequest;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "GitHub", description = "GitHub token, repositories and pull request sync")
@RequestMapping("/api/v1")
public interface GithubApi {

    @Operation(
            summary = "Save the GitHub token",
            description = "The token is checked against GitHub and stored encrypted. It is never returned.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token saved"),
            @ApiResponse(responseCode = "422", description = "GitHub rejected the token"),
            @ApiResponse(responseCode = "503", description = "The server has no encryption key")
    })
    @PutMapping("/github/token")
    GithubConnectionResponse saveToken(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody SaveGithubTokenRequest request
    );

    @Operation(summary = "Whether a GitHub token is saved", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/github/connection")
    GithubConnectionResponse connection(@Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "Remove the GitHub token", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/github/token")
    ResponseEntity<Void> removeToken(@Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "Repositories the token can reach", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/github/repositories")
    List<RepositoryResponse> accessibleRepositories(@Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "Repositories connected to a workspace", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/workspaces/{workspaceId}/repositories")
    List<RepositoryResponse> repositories(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId
    );

    @Operation(
            summary = "Connect a repository to a workspace",
            description = "Owners and admins. Uses the caller's token, which is also used later to poll pull requests.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/workspaces/{workspaceId}/repositories")
    ResponseEntity<RepositoryResponse> connect(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ConnectRepositoryRequest request
    );

    @Operation(summary = "Disconnect a repository from a workspace", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/workspaces/{workspaceId}/repositories/{repositoryId}")
    ResponseEntity<Void> disconnect(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID repositoryId
    );

    @Operation(
            summary = "Poll pull requests now",
            description = "The server also polls on a schedule, because GitHub cannot call a local server. "
                    + "Per-repository failures are reported in lastSyncError and do not fail the call.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/workspaces/{workspaceId}/repositories/sync")
    List<RepositoryResponse> sync(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId
    );
}
