package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueTokensServiceTest {

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

    private IssueTokensService service;

    @BeforeEach
    void setUp() {
        service = new IssueTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                REFRESH_TTL
        );
    }

    @Test
    void issuesAccessTokenAndStoresHashedRefreshTokenInANewFamily() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        AccessToken accessToken = new AccessToken("jwt", NOW.plusSeconds(900));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenCodec.generate()).thenReturn("raw-refresh");
        when(refreshTokenCodec.hash("raw-refresh")).thenReturn("refresh-hash");
        when(accessTokenIssuer.issue(user)).thenReturn(accessToken);

        AuthTokens tokens = service.execute(new IssueTokensCommand(user.getId()));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(stored.getUserId()).isEqualTo(user.getId());
        assertThat(stored.getTokenHash()).isEqualTo("refresh-hash");
        assertThat(stored.getFamilyId()).isNotNull();
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(stored.isRevoked()).isFalse();

        assertThat(tokens.user()).isSameAs(user);
        assertThat(tokens.accessToken()).isEqualTo(accessToken);
        assertThat(tokens.refreshToken()).isEqualTo("raw-refresh");
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
    }

    @Test
    void failsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new IssueTokensCommand(userId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
