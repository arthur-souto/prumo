package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.command.StartOAuthLoginCommand;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartOAuthLoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration CODE_TTL = Duration.ofSeconds(60);

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserIdentityRepositoryPort identityRepository;

    @Mock
    private AuthorizationCodeRepositoryPort codeRepository;

    @Mock
    private AuthorizationCodeCodecPort codeCodec;

    private StartOAuthLoginService service;

    @BeforeEach
    void setUp() {
        service = new StartOAuthLoginService(
                userRepository,
                identityRepository,
                codeRepository,
                codeCodec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                CODE_TTL
        );
    }

    @Test
    void registersANewUserLinksTheIdentityAndIssuesALoginCode() {
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("octo@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(codeCodec.generate()).thenReturn("raw-code");
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");

        String code = service.execute(new StartOAuthLoginCommand(
                AuthProvider.GITHUB,
                "42",
                "Octo@Example.com",
                " The Octocat ",
                "https://avatars.example/octo.png"
        ));

        assertThat(code).isEqualTo("raw-code");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();
        assertThat(created.getName()).isEqualTo("The Octocat");
        assertThat(created.getEmail()).isEqualTo("octo@example.com");
        assertThat(created.getAvatarUrl()).isEqualTo("https://avatars.example/octo.png");
        assertThat(created.hasPassword()).isFalse();

        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getUserId()).isEqualTo(created.getId());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(identityCaptor.getValue().getProviderId()).isEqualTo("42");

        ArgumentCaptor<AuthorizationCode> codeCaptor = ArgumentCaptor.forClass(AuthorizationCode.class);
        verify(codeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getUserId()).isEqualTo(created.getId());
        assertThat(codeCaptor.getValue().getPurpose()).isEqualTo(AuthorizationCodePurpose.LOGIN);
        assertThat(codeCaptor.getValue().getCodeHash()).isEqualTo("code-hash");
        assertThat(codeCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(CODE_TTL));
    }

    @Test
    void logsInTheUserOfALinkedIdentityEvenWithoutAnEmail() {
        User existing = User.registerLocal("Octo", "octo@example.com", "hash");
        UserIdentity identity = UserIdentity.link(existing.getId(), AuthProvider.GITHUB, "42", NOW);
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.of(identity));
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(codeCodec.generate()).thenReturn("raw-code");
        when(codeCodec.hash("raw-code")).thenReturn("code-hash");

        service.execute(new StartOAuthLoginCommand(AuthProvider.GITHUB, "42", null, "Octo", null));

        verify(userRepository, never()).save(any(User.class));
        verify(identityRepository, never()).save(any(UserIdentity.class));
        ArgumentCaptor<AuthorizationCode> codeCaptor = ArgumentCaptor.forClass(AuthorizationCode.class);
        verify(codeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getUserId()).isEqualTo(existing.getId());
    }

    @Test
    void refusesAnEmailThatBelongsToAnotherAccount() {
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("octo@example.com")).thenReturn(true);

        assertRejected(new StartOAuthLoginCommand(AuthProvider.GITHUB, "42", "octo@example.com", "Octo", null),
                OAuthLoginException.Reason.EMAIL_ALREADY_REGISTERED);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(codeRepository);
    }

    @Test
    void refusesANewUserWhenThereIsNoVerifiedEmail() {
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.empty());

        assertRejected(new StartOAuthLoginCommand(AuthProvider.GITHUB, "42", null, "Octo", null),
                OAuthLoginException.Reason.EMAIL_UNAVAILABLE);
        assertRejected(new StartOAuthLoginCommand(AuthProvider.GITHUB, "42", " ", "Octo", null),
                OAuthLoginException.Reason.EMAIL_UNAVAILABLE);

        verifyNoInteractions(userRepository, codeRepository);
    }

    @Test
    void refusesADisabledAccount() {
        User disabled = User.registerExternal("Octo", "octo@example.com", null);
        disabled.disable();
        UserIdentity identity = UserIdentity.link(disabled.getId(), AuthProvider.GITHUB, "42", NOW);
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.of(identity));
        when(userRepository.findById(disabled.getId())).thenReturn(Optional.of(disabled));

        assertRejected(new StartOAuthLoginCommand(AuthProvider.GITHUB, "42", "octo@example.com", "Octo", null),
                OAuthLoginException.Reason.ACCOUNT_DISABLED);

        verifyNoInteractions(codeRepository);
    }

    private void assertRejected(StartOAuthLoginCommand command, OAuthLoginException.Reason reason) {
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OAuthLoginException.class)
                .extracting(exception -> ((OAuthLoginException) exception).getReason())
                .isEqualTo(reason);
    }
}
