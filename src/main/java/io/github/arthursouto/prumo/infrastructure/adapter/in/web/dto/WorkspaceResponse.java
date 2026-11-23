package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A workspace as seen by the authenticated user")
public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        String description,
        WorkspaceRole role,
        int memberCount,
        Instant createdAt
) {

    public static WorkspaceResponse from(Workspace workspace, UUID viewerId) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getDescription(),
                workspace.roleOf(viewerId).orElse(null),
                workspace.getMembers().size(),
                workspace.getCreatedAt()
        );
    }
}
