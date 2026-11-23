package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.Role;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2020-06-01T00:00:00Z");

    @Test
    void toEntityCopiesEveryField() {
        UUID id = UUID.randomUUID();
        User user = User.restore(
                id,
                "Arthur",
                "arthur@prumo.io",
                "hash",
                Role.ADMIN,
                AuthProvider.GITHUB,
                "12345",
                "avatar",
                false,
                CREATED_AT,
                UPDATED_AT
        );

        UserEntity entity = UserMapper.toEntity(user);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Arthur");
        assertThat(entity.getEmail()).isEqualTo("arthur@prumo.io");
        assertThat(entity.getPasswordHash()).isEqualTo("hash");
        assertThat(entity.getRole()).isEqualTo(Role.ADMIN);
        assertThat(entity.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(entity.getProviderId()).isEqualTo("12345");
        assertThat(entity.getAvatarUrl()).isEqualTo("avatar");
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void toDomainCopiesEveryField() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setName("Arthur");
        entity.setEmail("arthur@prumo.io");
        entity.setPasswordHash(null);
        entity.setRole(Role.USER);
        entity.setAuthProvider(AuthProvider.GITHUB);
        entity.setProviderId("12345");
        entity.setAvatarUrl("avatar");
        entity.setEnabled(true);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);

        User user = UserMapper.toDomain(entity);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Arthur");
        assertThat(user.getEmail()).isEqualTo("arthur@prumo.io");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(user.getProviderId()).isEqualTo("12345");
        assertThat(user.getAvatarUrl()).isEqualTo("avatar");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void roundTripPreservesTheUser() {
        User original = User.registerLocal("Arthur", "arthur@prumo.io", "hash");

        User restored = UserMapper.toDomain(UserMapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}
