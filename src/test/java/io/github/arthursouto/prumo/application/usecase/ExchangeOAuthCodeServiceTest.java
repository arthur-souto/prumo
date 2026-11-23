package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidAuthorizationCodeException;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ExchangeOAuthCodeCommand;
import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeOAuthCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationCodeRepositoryPort codeRepository;

    @Mock
    private AuthorizationCodeCodecPort codeCodec;

    @Mock
    private IssueTokensUseCase issueTokens;

    private ExchangeOAuthCodeService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeOAuthCodeService(
                userRepository,
                codeRepository,
                codeCodec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                issueTokens
        );
    }

    @Test
    void consumesTheCodeAndIssuesTokens() {
        User user = User.registerExternal("Octo", "octo@example.com", null);
        AuthorizationCode code = AuthorizationCode.issue(user.getId(), AuthorizationCodePurpose.LOGIN, "code-hash", NOW.minusSeconds(10), NOW.plusSeconds(50));
        AuthTokens tokens = new AuthTokens(user, new AccessToken("jwt", NOW.plusSeconds(900)), "refresh", NOW.plusSeconds(1000));
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.of(code));
        when(codeRepository.markUsed(code.getId(), NOW)).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(issueTokens.execute(new IssueTokensCommand(user.getId()))).thenReturn(tokens);

        AuthTokens result = service.execute(new ExchangeOAuthCodeCommand("raw-code"));

        assertThat(result).isSameAs(tokens);
        verify(codeRepository).markUsed(code.getId(), NOW);
    }

    @Test
    void rejectsAnUnknownCode() {
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand("raw-code")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verifyNoInteractions(issueTokens);
    }

    @Test
    void rejectsAnExpiredCode() {
        AuthorizationCode expired = AuthorizationCode.issue(UUID.randomUUID(), AuthorizationCodePurpose.LOGIN, "code-hash", NOW.minusSeconds(120), NOW.minusSeconds(1));
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand("raw-code")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verify(codeRepository, never()).markUsed(any(UUID.class), any(Instant.class));
        verifyNoInteractions(issueTokens);
    }

    @Test
    void rejectsAnAlreadyUsedCode() {
        AuthorizationCode used = AuthorizationCode.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuthorizationCodePurpose.LOGIN, "code-hash",
                NOW.minusSeconds(30),
                NOW.plusSeconds(30),
                NOW.minusSeconds(5)
        );
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand("raw-code")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verify(codeRepository, never()).markUsed(any(UUID.class), any(Instant.class));
        verifyNoInteractions(issueTokens);
    }

    @Test
    void rejectsWhenAConcurrentRequestConsumedTheCodeFirst() {
        AuthorizationCode code = AuthorizationCode.issue(UUID.randomUUID(), AuthorizationCodePurpose.LOGIN, "code-hash", NOW.minusSeconds(10), NOW.plusSeconds(50));
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.of(code));
        when(codeRepository.markUsed(code.getId(), NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand("raw-code")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verifyNoInteractions(issueTokens);
    }

    @Test
    void rejectsACodeOfADisabledUser() {
        User user = User.registerExternal("Octo", "octo@example.com", null);
        user.disable();
        AuthorizationCode code = AuthorizationCode.issue(user.getId(), AuthorizationCodePurpose.LOGIN, "code-hash", NOW.minusSeconds(10), NOW.plusSeconds(50));
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");
        when(codeRepository.findByCodeHash("code-hash")).thenReturn(Optional.of(code));
        when(codeRepository.markUsed(code.getId(), NOW)).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand("raw-code")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verifyNoInteractions(issueTokens);
    }

    @Test
    void rejectsMissingOrBlankCodesWithoutTouchingTheStore() {
        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand(null)))
                .isInstanceOf(InvalidAuthorizationCodeException.class);
        assertThatThrownBy(() -> service.execute(new ExchangeOAuthCodeCommand(" ")))
                .isInstanceOf(InvalidAuthorizationCodeException.class);

        verifyNoInteractions(codeRepository, userRepository, issueTokens);
    }
}
