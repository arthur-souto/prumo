package io.github.arthursouto.prumo.domain.model.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Instant PAST = Instant.parse("2020-01-01T00:00:00Z");

    @Test
    void registerLocalCreatesEnabledUserWithDefaultRole() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hash");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("Arthur");
        assertThat(user.getEmail()).isEqualTo("arthur@prumo.io");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull().isEqualTo(user.getUpdatedAt());
    }

    @Test
    void registerExternalCreatesUserWithoutPassword() {
        User user = User.registerExternal("Arthur", "arthur@prumo.io", "https://avatars.example/arthur.png");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getAvatarUrl()).isEqualTo("https://avatars.example/arthur.png");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void registerGeneratesDistinctIds() {
        User first = User.registerLocal("A", "a@prumo.io", "hash");
        User second = User.registerLocal("B", "b@prumo.io", "hash");

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void restoreKeepsEveryValue() {
        UUID id = UUID.randomUUID();
        Instant updatedAt = PAST.plusSeconds(60);

        User user = User.restore(
                id,
                "Arthur",
                "arthur@prumo.io",
                "hash",
                Role.ADMIN,
                "avatar",
                false,
                PAST,
                updatedAt
        );

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Arthur");
        assertThat(user.getEmail()).isEqualTo("arthur@prumo.io");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getAvatarUrl()).isEqualTo("avatar");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getCreatedAt()).isEqualTo(PAST);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void renameChangesNameAndTouchesUpdatedAt() {
        User user = restored();

        user.rename("New Name");

        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getUpdatedAt()).isAfter(PAST);
        assertThat(user.getCreatedAt()).isEqualTo(PAST);
    }

    @Test
    void changeRoleChangesRoleAndTouchesUpdatedAt() {
        User user = restored();

        user.changeRole(Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getUpdatedAt()).isAfter(PAST);
    }

    @Test
    void changePasswordHashChangesHashAndTouchesUpdatedAt() {
        User user = restored();

        user.changePasswordHash("new-hash");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.getUpdatedAt()).isAfter(PAST);
    }

    @Test
    void changeAvatarUrlChangesAvatarAndTouchesUpdatedAt() {
        User user = restored();

        user.changeAvatarUrl("new-avatar");

        assertThat(user.getAvatarUrl()).isEqualTo("new-avatar");
        assertThat(user.getUpdatedAt()).isAfter(PAST);
    }

    @Test
    void disableAndEnableToggleEnabledAndTouchUpdatedAt() {
        User user = restored();

        user.disable();

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getUpdatedAt()).isAfter(PAST);

        Instant afterDisable = user.getUpdatedAt();
        user.enable();

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(afterDisable);
    }

    @Test
    void hasPasswordIsFalseWhenHashIsNull() {
        User user = User.restore(
                UUID.randomUUID(),
                "Arthur",
                "arthur@prumo.io",
                null,
                Role.USER,
                null,
                true,
                PAST,
                PAST
        );

        assertThat(user.hasPassword()).isFalse();
    }

    private static User restored() {
        return User.restore(
                UUID.randomUUID(),
                "Arthur",
                "arthur@prumo.io",
                "hash",
                Role.USER,
                null,
                true,
                PAST,
                PAST
        );
    }
}
