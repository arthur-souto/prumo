package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.UserPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private UserJpaRepository repository;

    private UserPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserPersistenceAdapter(repository);
    }

    @Test
    void savePersistsEntityAndReturnsDomainUser() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hash");
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = adapter.save(user);

        assertThat(saved).usingRecursiveComparison().isEqualTo(user);
        verify(repository).save(any(UserEntity.class));
    }

    @Test
    void findByIdMapsEntityToDomain() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hash");
        when(repository.findById(user.getId())).thenReturn(Optional.of(UserMapper.toEntity(user)));

        Optional<User> found = adapter.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void findByEmailMapsEntityToDomain() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hash");
        when(repository.findByEmail("arthur@prumo.io")).thenReturn(Optional.of(UserMapper.toEntity(user)));

        Optional<User> found = adapter.findByEmail("arthur@prumo.io");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("arthur@prumo.io");
    }

    @Test
    void findByEmailReturnsEmptyWhenMissing() {
        when(repository.findByEmail("missing@prumo.io")).thenReturn(Optional.empty());

        assertThat(adapter.findByEmail("missing@prumo.io")).isEmpty();
    }

    @Test
    void findByProviderMapsEntityToDomain() {
        User user = User.registerExternal(
                "Arthur",
                "arthur@prumo.io",
                AuthProvider.GITHUB,
                "12345",
                null
        );
        when(repository.findByAuthProviderAndProviderId(AuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.of(UserMapper.toEntity(user)));

        Optional<User> found = adapter.findByProvider(AuthProvider.GITHUB, "12345");

        assertThat(found).isPresent();
        assertThat(found.get().getProviderId()).isEqualTo("12345");
    }

    @Test
    void findByProviderReturnsEmptyWhenMissing() {
        when(repository.findByAuthProviderAndProviderId(AuthProvider.GITHUB, "0"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByProvider(AuthProvider.GITHUB, "0")).isEmpty();
    }

    @Test
    void existsByEmailDelegatesToRepository() {
        when(repository.existsByEmail("arthur@prumo.io")).thenReturn(true);
        when(repository.existsByEmail("missing@prumo.io")).thenReturn(false);

        assertThat(adapter.existsByEmail("arthur@prumo.io")).isTrue();
        assertThat(adapter.existsByEmail("missing@prumo.io")).isFalse();
    }
}
