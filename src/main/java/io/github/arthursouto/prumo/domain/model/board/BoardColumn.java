package io.github.arthursouto.prumo.domain.model.board;

import java.time.Instant;
import java.util.UUID;

public final class BoardColumn {

    private final UUID id;
    private final UUID workspaceId;
    private String name;
    private final ColumnKind kind;
    private int position;
    private final Instant createdAt;

    private BoardColumn(UUID id, UUID workspaceId, String name, ColumnKind kind, int position, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.kind = kind;
        this.position = position;
        this.createdAt = createdAt;
    }

    public static BoardColumn create(UUID workspaceId, String name, ColumnKind kind, int position) {
        return new BoardColumn(UUID.randomUUID(), workspaceId, name, kind, position, Instant.now());
    }

    public static BoardColumn restore(UUID id,
                                      UUID workspaceId,
                                      String name,
                                      ColumnKind kind,
                                      int position,
                                      Instant createdAt) {
        return new BoardColumn(id, workspaceId, name, kind, position, createdAt);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public ColumnKind getKind() {
        return kind;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
