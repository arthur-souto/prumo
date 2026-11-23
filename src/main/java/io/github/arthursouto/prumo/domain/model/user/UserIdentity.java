package io.github.arthursouto.prumo.domain.model.user;

import java.time.Instant;
import java.util.UUID;

public final class UserIdentity {

    private final UUID id;
    private final UUID userId;
    private final AuthProvider provider;
    private final String providerId;
    private final Instant createdAt;

    private UserIdentity(UUID id, UUID userId, AuthProvider provider, String providerId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = createdAt;
    }

    public static UserIdentity link(UUID userId, AuthProvider provider, String providerId, Instant createdAt) {
        return new UserIdentity(UUID.randomUUID(), userId, provider, providerId, createdAt);
    }

    public static UserIdentity restore(UUID id,
                                       UUID userId,
                                       AuthProvider provider,
                                       String providerId,
                                       Instant createdAt) {
        return new UserIdentity(id, userId, provider, providerId, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
