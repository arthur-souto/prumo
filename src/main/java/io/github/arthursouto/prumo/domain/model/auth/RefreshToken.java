package io.github.arthursouto.prumo.domain.model.auth;

import java.time.Instant;
import java.util.UUID;

public final class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshToken(UUID id,
                         UUID userId,
                         UUID familyId,
                         String tokenHash,
                         Instant createdAt,
                         Instant expiresAt,
                         Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UUID userId, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, familyId, tokenHash, createdAt, expiresAt, null);
    }

    public static RefreshToken restore(UUID id,
                                       UUID userId,
                                       UUID familyId,
                                       String tokenHash,
                                       Instant createdAt,
                                       Instant expiresAt,
                                       Instant revokedAt) {
        return new RefreshToken(id, userId, familyId, tokenHash, createdAt, expiresAt, revokedAt);
    }

    public void revoke(Instant at) {
        this.revokedAt = at;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
