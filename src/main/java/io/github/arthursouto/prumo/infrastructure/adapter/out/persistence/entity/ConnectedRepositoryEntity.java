package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_repositories")
@Getter
@Setter
@NoArgsConstructor
public class ConnectedRepositoryEntity {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @Column(name = "html_url", nullable = false)
    private String htmlUrl;

    @Column(name = "connected_by", nullable = false)
    private UUID connectedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_error")
    private String lastSyncError;

    private String etag;
}
