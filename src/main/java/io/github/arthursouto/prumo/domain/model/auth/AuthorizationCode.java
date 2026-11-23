package io.github.arthursouto.prumo.domain.model.auth;

import java.time.Instant;
import java.util.UUID;

public final class AuthorizationCode {

    private final UUID id;
    private final UUID userId;
    private final AuthorizationCodePurpose purpose;
    private final String codeHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant usedAt;

    private AuthorizationCode(UUID id,
                              UUID userId,
                              AuthorizationCodePurpose purpose,
                              String codeHash,
                              Instant createdAt,
                              Instant expiresAt,
                              Instant usedAt) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public static AuthorizationCode issue(UUID userId,
                                          AuthorizationCodePurpose purpose,
                                          String codeHash,
                                          Instant createdAt,
                                          Instant expiresAt) {
        return new AuthorizationCode(UUID.randomUUID(), userId, purpose, codeHash, createdAt, expiresAt, null);
    }

    public static AuthorizationCode restore(UUID id,
                                            UUID userId,
                                            AuthorizationCodePurpose purpose,
                                            String codeHash,
                                            Instant createdAt,
                                            Instant expiresAt,
                                            Instant usedAt) {
        return new AuthorizationCode(id, userId, purpose, codeHash, createdAt, expiresAt, usedAt);
    }

    public boolean isUsed() {
        return usedAt != null;
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

    public AuthorizationCodePurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
