package io.github.arthursouto.prumo.domain.model.workspace;

import java.time.Instant;
import java.util.UUID;

public final class WorkspaceMember {

    private final UUID id;
    private final UUID workspaceId;
    private final UUID userId;
    private final WorkspaceRole role;
    private final Instant createdAt;

    private WorkspaceMember(UUID id, UUID workspaceId, UUID userId, WorkspaceRole role, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static WorkspaceMember restore(UUID id,
                                          UUID workspaceId,
                                          UUID userId,
                                          WorkspaceRole role,
                                          Instant createdAt) {
        return new WorkspaceMember(id, workspaceId, userId, role, createdAt);
    }

    static WorkspaceMember join(UUID workspaceId, UUID userId, WorkspaceRole role, Instant createdAt) {
        return new WorkspaceMember(UUID.randomUUID(), workspaceId, userId, role, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
