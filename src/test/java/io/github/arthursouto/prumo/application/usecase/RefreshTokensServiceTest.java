package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidRefreshTokenException;
import io.github.arthursouto.prumo.application.port.in.command.RefreshTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.domain.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokensServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Mock
    private AccessTokenIssuerPort accessTokenIssuer;

    @Mock
    private RefreshTokenCodecPort refreshTokenCodec;

    private RefreshTokensService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                REFRESH_TTL
        );
    }

    @Test
    void rotatesTheRefreshTokenKeepingTheFamilyAndIssuesANewAccessToken() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        UUID familyId = UUID.randomUUID();
        RefreshToken current = RefreshToken.issue(user.getId(), familyId, "old-hash", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        AccessToken accessToken = new AccessToken("jwt", NOW.plusSeconds(900));
        when(refreshTokenCodec.hash("old-raw")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(current));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenCodec.generate()).thenReturn("new-raw");
        when(refreshTokenCodec.hash("new-raw")).thenReturn("new-hash");
        when(accessTokenIssuer.issue(user)).thenReturn(accessToken);

        AuthTokens tokens = service.execute(new RefreshTokensCommand("old-raw"));

        ArgumentCaptor<RefreshToken> rotatedCurrent = ArgumentCaptor.forClass(RefreshToken.class);
        ArgumentCaptor<RefreshToken> rotatedNext = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).rotate(rotatedCurrent.capture(), rotatedNext.capture());
        assertThat(rotatedCurrent.getValue()).isSameAs(current);
        assertThat(current.isRevoked()).isTrue();
        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(rotatedNext.getValue().getFamilyId()).isEqualTo(familyId);
        assertThat(rotatedNext.getValue().getTokenHash()).isEqualTo("new-hash");
        assertThat(rotatedNext.getValue().getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(rotatedNext.getValue().isRevoked()).isFalse();

        assertThat(tokens.user()).isSameAs(user);
        assertThat(tokens.accessToken()).isEqualTo(accessToken);
        assertThat(tokens.refreshToken()).isEqualTo("new-raw");
    }

    @Test
    void reusingARevokedTokenRevokesTheWholeFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken reused = RefreshToken.issue(UUID.randomUUID(), familyId, "old-hash", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        reused.revoke(NOW.minusSeconds(30));
        when(refreshTokenCodec.hash("old-raw")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("old-raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeFamily(familyId, NOW);
        verify(refreshTokenRepository, never()).rotate(any(RefreshToken.class), any(RefreshToken.class));
    }

    @Test
    void rejectsAnExpiredToken() {
        RefreshToken expired = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "old-hash", NOW.minusSeconds(120), NOW.minusSeconds(1));
        when(refreshTokenCodec.hash("old-raw")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("old-raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).rotate(any(RefreshToken.class), any(RefreshToken.class));
    }

    @Test
    void rejectsAnUnknownToken() {
        when(refreshTokenCodec.hash("unknown")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("unknown")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsATokenOfADisabledUser() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        user.disable();
        RefreshToken current = RefreshToken.issue(user.getId(), UUID.randomUUID(), "old-hash", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(refreshTokenCodec.hash("old-raw")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(current));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("old-raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).rotate(any(RefreshToken.class), any(RefreshToken.class));
    }

    @Test
    void rejectsATokenWhoseUserNoLongerExists() {
        RefreshToken current = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "old-hash", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(refreshTokenCodec.hash("old-raw")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(current));
        when(userRepository.findById(current.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("old-raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsMissingOrBlankTokensWithoutTouchingTheStore() {
        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand(null)))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.execute(new RefreshTokensCommand("  ")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenRepository, userRepository, accessTokenIssuer);
    }
}
