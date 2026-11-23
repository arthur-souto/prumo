package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.AuthorizationCodePersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.AuthorizationCodeEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.AuthorizationCodeJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.AuthorizationCodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationCodePersistenceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AuthorizationCodeJpaRepository repository;

    private AuthorizationCodePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthorizationCodePersistenceAdapter(repository);
    }

    @Test
    void mapperRoundTripPreservesTheCode() {
        AuthorizationCode original = AuthorizationCode.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuthorizationCodePurpose.LOGIN, "hash",
                NOW,
                NOW.plusSeconds(60),
                NOW.plusSeconds(5)
        );

        AuthorizationCode restored = AuthorizationCodeMapper.toDomain(AuthorizationCodeMapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void savePersistsEntityAndReturnsDomainCode() {
        AuthorizationCode code = AuthorizationCode.issue(UUID.randomUUID(), AuthorizationCodePurpose.LOGIN, "hash", NOW, NOW.plusSeconds(60));
        when(repository.save(any(AuthorizationCodeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.save(code)).usingRecursiveComparison().isEqualTo(code);
    }

    @Test
    void findByCodeHashMapsTheEntityOrReturnsEmpty() {
        AuthorizationCode code = AuthorizationCode.issue(UUID.randomUUID(), AuthorizationCodePurpose.LOGIN, "hash", NOW, NOW.plusSeconds(60));
        when(repository.findByCodeHash("hash")).thenReturn(Optional.of(AuthorizationCodeMapper.toEntity(code)));
        when(repository.findByCodeHash("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findByCodeHash("hash")).isPresent();
        assertThat(adapter.findByCodeHash("missing")).isEmpty();
    }

    @Test
    void markUsedIsTrueOnlyWhenTheCodeWasStillUnused() {
        UUID id = UUID.randomUUID();
        when(repository.markUsed(id, NOW)).thenReturn(1, 0);

        assertThat(adapter.markUsed(id, NOW)).isTrue();
        assertThat(adapter.markUsed(id, NOW)).isFalse();
        verify(repository, org.mockito.Mockito.times(2)).markUsed(id, NOW);
    }
}
