package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.UserIdentityPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserIdentityEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserIdentityJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.UserIdentityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdentityPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserIdentityJpaRepository repository;

    private UserIdentityPersistenceAdapter adapter;
    private UserIdentity identity;

    @BeforeEach
    void setUp() {
        adapter = new UserIdentityPersistenceAdapter(repository);
        identity = UserIdentity.link(UUID.randomUUID(), AuthProvider.GITHUB, "42", NOW);
    }

    @Test
    void mapperRoundTripPreservesTheIdentity() {
        UserIdentityEntity entity = UserIdentityMapper.toEntity(identity);

        assertThat(entity.getProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(UserIdentityMapper.toDomain(entity)).usingRecursiveComparison().isEqualTo(identity);
    }

    @Test
    void savePersistsAndReturnsTheDomainIdentity() {
        when(repository.save(any(UserIdentityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.save(identity)).usingRecursiveComparison().isEqualTo(identity);
    }

    @Test
    void findersMapEntitiesOrReturnEmpty() {
        when(repository.findByProviderAndProviderId(AuthProvider.GITHUB, "42"))
                .thenReturn(Optional.of(UserIdentityMapper.toEntity(identity)));
        when(repository.findByProviderAndProviderId(AuthProvider.GITHUB, "0")).thenReturn(Optional.empty());
        when(repository.findByUserIdAndProvider(identity.getUserId(), AuthProvider.GITHUB))
                .thenReturn(Optional.of(UserIdentityMapper.toEntity(identity)));
        when(repository.findByUserId(identity.getUserId())).thenReturn(List.of(UserIdentityMapper.toEntity(identity)));

        assertThat(adapter.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).isPresent();
        assertThat(adapter.findByProviderAndProviderId(AuthProvider.GITHUB, "0")).isEmpty();
        assertThat(adapter.findByUserIdAndProvider(identity.getUserId(), AuthProvider.GITHUB)).isPresent();
        assertThat(adapter.findByUserId(identity.getUserId())).hasSize(1);
    }

    @Test
    void deleteDelegatesToTheRepository() {
        adapter.delete(identity.getId());

        verify(repository).deleteById(identity.getId());
    }
}
