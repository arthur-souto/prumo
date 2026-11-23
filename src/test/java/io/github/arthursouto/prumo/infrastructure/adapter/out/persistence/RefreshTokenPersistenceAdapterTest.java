package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.RefreshTokenPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.RefreshTokenJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RefreshTokenJpaRepository repository;

    private RefreshTokenPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenPersistenceAdapter(repository);
    }

    @Test
    void savePersistsEntityAndReturnsDomainToken() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));
        when(repository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken saved = adapter.save(token);

        assertThat(saved).usingRecursiveComparison().isEqualTo(token);
    }

    @Test
    void findByTokenHashMapsEntityToDomain() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(RefreshTokenMapper.toEntity(token)));

        Optional<RefreshToken> found = adapter.findByTokenHash("hash");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(token.getId());
    }

    @Test
    void findByTokenHashReturnsEmptyWhenMissing() {
        when(repository.findByTokenHash("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findByTokenHash("missing")).isEmpty();
    }

    @Test
    void rotateSavesTheRevokedCurrentTokenAndTheNextOne() {
        RefreshToken current = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "old", NOW, NOW.plusSeconds(60));
        current.revoke(NOW);
        RefreshToken next = RefreshToken.issue(current.getUserId(), current.getFamilyId(), "new", NOW, NOW.plusSeconds(60));

        adapter.rotate(current, next);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(RefreshTokenEntity::getTokenHash).containsExactly("old", "new");
        assertThat(captor.getAllValues().get(0).getRevokedAt()).isEqualTo(NOW);
        assertThat(captor.getAllValues().get(1).getRevokedAt()).isNull();
    }

    @Test
    void revokeFamilyDelegatesToTheRepository() {
        UUID familyId = UUID.randomUUID();

        adapter.revokeFamily(familyId, NOW);

        verify(repository).revokeFamily(familyId, NOW);
    }
}
