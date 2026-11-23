package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.RevokeRefreshTokenCommand;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeRefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Mock
    private RefreshTokenCodecPort refreshTokenCodec;

    private RevokeRefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RevokeRefreshTokenService(
                refreshTokenRepository,
                refreshTokenCodec,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void revokesTheWholeFamilyOfAKnownToken() {
        UUID familyId = UUID.randomUUID();
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), familyId, "hash", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(refreshTokenCodec.hash("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        service.execute(new RevokeRefreshTokenCommand("raw"));

        verify(refreshTokenRepository).revokeFamily(familyId, NOW);
    }

    @Test
    void ignoresAnUnknownToken() {
        when(refreshTokenCodec.hash("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        service.execute(new RevokeRefreshTokenCommand("raw"));

        verify(refreshTokenRepository, never()).revokeFamily(any(UUID.class), any(Instant.class));
    }

    @Test
    void ignoresMissingOrBlankTokens() {
        service.execute(new RevokeRefreshTokenCommand(null));
        service.execute(new RevokeRefreshTokenCommand(" "));

        verifyNoInteractions(refreshTokenRepository, refreshTokenCodec);
    }
}
