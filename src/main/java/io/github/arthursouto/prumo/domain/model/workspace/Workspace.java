package io.github.arthursouto.prumo.domain.model.workspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Workspace {

    private final UUID id;
    private String name;
    private final String slug;
    private String description;
    private final UUID ownerId;
    private final List<WorkspaceMember> members;
    private final Instant createdAt;
    private Instant updatedAt;

    private Workspace(UUID id,
                       String name,
                       String slug,
                       String description,
                       UUID ownerId,
                       List<WorkspaceMember> members,
                       Instant createdAt,
                       Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.ownerId = ownerId;
        this.members = List.copyOf(members);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Workspace create(String name, String slug, String description, UUID ownerId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        WorkspaceMember owner = WorkspaceMember.join(id, ownerId, WorkspaceRole.OWNER, now);

        return new Workspace(
                id,
                name,
                slug,
                description,
                ownerId,
                List.of(owner),
                now,
                now
        );
    }

    public static Workspace restore(UUID id,
                                    String name,
                                    String slug,
                                    String description,
                                    UUID ownerId,
                                    List<WorkspaceMember> members,
                                    Instant createdAt,
                                    Instant updatedAt) {
        return new Workspace(
                id,
                name,
                slug,
                description,
                ownerId,
                members,
                createdAt,
                updatedAt
        );
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public boolean canEdit(UUID userId) {
        return roleOf(userId)
                .filter(role -> role == WorkspaceRole.OWNER || role == WorkspaceRole.ADMIN)
                .isPresent();
    }

    public boolean canDelete(UUID userId) {
        return roleOf(userId).filter(role -> role == WorkspaceRole.OWNER).isPresent();
    }

    public Optional<WorkspaceRole> roleOf(UUID userId) {
        return members.stream()
                .filter(member -> member.getUserId().equals(userId))
                .map(WorkspaceMember::getRole)
                .findFirst();
    }

    public boolean hasMember(UUID userId) {
        return roleOf(userId).isPresent();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public List<WorkspaceMember> getMembers() {
        return members;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
