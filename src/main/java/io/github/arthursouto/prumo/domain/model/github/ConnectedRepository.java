package io.github.arthursouto.prumo.domain.model.github;

import java.time.Instant;
import java.util.UUID;

public final class ConnectedRepository {

    private final UUID id;
    private final UUID workspaceId;
    private final String owner;
    private final String name;
    private final String defaultBranch;
    private final String htmlUrl;
    private final UUID connectedBy;
    private final Instant createdAt;
    private Instant lastSyncedAt;
    private String lastSyncError;
    private String etag;

    private ConnectedRepository(UUID id,
                                UUID workspaceId,
                                String owner,
                                String name,
                                String defaultBranch,
                                String htmlUrl,
                                UUID connectedBy,
                                Instant createdAt,
                                Instant lastSyncedAt,
                                String lastSyncError,
                                String etag) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.owner = owner;
        this.name = name;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
        this.connectedBy = connectedBy;
        this.createdAt = createdAt;
        this.lastSyncedAt = lastSyncedAt;
        this.lastSyncError = lastSyncError;
        this.etag = etag;
    }

    public static ConnectedRepository connect(UUID workspaceId,
                                              String owner,
                                              String name,
                                              String defaultBranch,
                                              String htmlUrl,
                                              UUID connectedBy) {
        return new ConnectedRepository(
                UUID.randomUUID(),
                workspaceId,
                owner,
                name,
                defaultBranch,
                htmlUrl,
                connectedBy,
                Instant.now(),
                null,
                null,
                null
        );
    }

    public static ConnectedRepository restore(UUID id,
                                              UUID workspaceId,
                                              String owner,
                                              String name,
                                              String defaultBranch,
                                              String htmlUrl,
                                              UUID connectedBy,
                                              Instant createdAt,
                                              Instant lastSyncedAt,
                                              String lastSyncError,
                                              String etag) {
        return new ConnectedRepository(
                id,
                workspaceId,
                owner,
                name,
                defaultBranch,
                htmlUrl,
                connectedBy,
                createdAt,
                lastSyncedAt,
                lastSyncError,
                etag
        );
    }

    public void markSynced(Instant at, String etag) {
        this.lastSyncedAt = at;
        this.lastSyncError = null;
        this.etag = etag;
    }

    public void markFailed(Instant at, String errorCode) {
        this.lastSyncedAt = at;
        this.lastSyncError = errorCode;
    }

    public String fullName() {
        return owner + "/" + name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public UUID getConnectedBy() {
        return connectedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getLastSyncError() {
        return lastSyncError;
    }

    public String getEtag() {
        return etag;
    }
}
