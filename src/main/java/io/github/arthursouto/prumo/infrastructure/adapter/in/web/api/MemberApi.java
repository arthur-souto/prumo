package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.MemberResponse;
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

@Tag(name = "Members", description = "People who belong to a workspace")
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
public interface MemberApi {

    @Operation(summary = "List the members of a workspace", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    List<MemberResponse> list(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable UUID workspaceId
    );
}
