package io.github.arthursouto.prumo.domain.model.user;

import java.time.Instant;
import java.util.UUID;

public final class User {

    private final UUID id;
    private String name;
    private final String email;
    private String passwordHash;
    private Role role;
    private String avatarUrl;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UUID id,
                 String name,
                 String email,
                 String passwordHash,
                 Role role,
                 String avatarUrl,
                 boolean enabled,
                 Instant createdAt,
                 Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User registerLocal(String name, String email, String passwordHash) {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                name,
                email,
                passwordHash,
                Role.USER,
                null,
                true,
                now,
                now
        );
    }

    public static User registerExternal(String name, String email, String avatarUrl) {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                name,
                email,
                null,
                Role.USER,
                avatarUrl,
                true,
                now,
                now
        );
    }

    public static User restore(UUID id,
                               String name,
                               String email,
                               String passwordHash,
                               Role role,
                               String avatarUrl,
                               boolean enabled,
                               Instant createdAt,
                               Instant updatedAt) {
        return new User(
                id,
                name,
                email,
                passwordHash,
                role,
                avatarUrl,
                enabled,
                createdAt,
                updatedAt
        );
    }

    public void rename(String name) {
        this.name = name;
        touch();
    }

    public void changeRole(Role role) {
        this.role = role;
        touch();
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        touch();
    }

    public void changeAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
