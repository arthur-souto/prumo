package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.command.CreateLinkTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetUserIdentitiesQuery;
import io.github.arthursouto.prumo.application.port.in.command.LinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.in.command.UnlinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkIdentityServicesTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserIdentityRepositoryPort identityRepository;

    @Mock
    private AuthorizationCodeRepositoryPort codeRepository;

    @Mock
    private AuthorizationCodeCodecPort codeCodec;

    private User user;
    private LinkOAuthIdentityService linkService;

    @BeforeEach
    void setUp() {
        user = User.registerLocal("Arthur", "arthur@prumo.io", "hash");
        linkService = new LinkOAuthIdentityService(userRepository, identityRepository, codeRepository, codeCodec, CLOCK);
    }

    @Test
    void createLinkTokenIssuesAShortLivedLinkCode() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeCodec.generate()).thenReturn("raw");
        when(codeCodec.hash("raw")).thenReturn("hashed");

        String token = new CreateLinkTokenService(userRepository, codeRepository, codeCodec, CLOCK, Duration.ofSeconds(60))
                .execute(new CreateLinkTokenCommand(user.getId()));

        assertThat(token).isEqualTo("raw");
        ArgumentCaptor<AuthorizationCode> captor = ArgumentCaptor.forClass(AuthorizationCode.class);
        verify(codeRepository).save(captor.capture());
        assertThat(captor.getValue().getPurpose()).isEqualTo(AuthorizationCodePurpose.LINK);
        assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void createLinkTokenRejectsUnknownOrDisabledUsers() {
        CreateLinkTokenService service = new CreateLinkTokenService(
                userRepository,
                codeRepository,
                codeCodec,
                CLOCK,
                Duration.ofSeconds(60)
        );
        user.disable();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.execute(new CreateLinkTokenCommand(user.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(codeRepository, never()).save(any(AuthorizationCode.class));
    }

    @Test
    void linkAttachesTheIdentityToTheUserOfTheToken() {
        stubToken(AuthorizationCodePurpose.LINK, NOW.plusSeconds(30), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.empty());
        when(identityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.GITHUB)).thenReturn(Optional.empty());

        linkService.execute(new LinkOAuthIdentityCommand("raw", AuthProvider.GITHUB, "42"));

        ArgumentCaptor<UserIdentity> captor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(captor.getValue().getProviderId()).isEqualTo("42");
    }

    @Test
    void linkRejectsALoginCodeUsedAsLinkToken() {
        stubToken(AuthorizationCodePurpose.LOGIN, NOW.plusSeconds(30), false);

        assertLinkRejected(OAuthLoginException.Reason.LINK_TOKEN_INVALID);
    }

    @Test
    void linkRejectsExpiredAndAlreadyUsedTokens() {
        stubToken(AuthorizationCodePurpose.LINK, NOW.minusSeconds(1), false);
        assertLinkRejected(OAuthLoginException.Reason.LINK_TOKEN_INVALID);

        stubToken(AuthorizationCodePurpose.LINK, NOW.plusSeconds(30), false);
        when(codeRepository.markUsed(any(UUID.class), any(Instant.class))).thenReturn(false);
        assertLinkRejected(OAuthLoginException.Reason.LINK_TOKEN_INVALID);
    }

    @Test
    void linkRejectsBlankTokens() {
        assertThatThrownBy(() -> linkService.execute(new LinkOAuthIdentityCommand(" ", AuthProvider.GITHUB, "42")))
                .isInstanceOf(OAuthLoginException.class);
    }

    @Test
    void linkRejectsAnIdentityOwnedByAnotherUser() {
        stubToken(AuthorizationCodePurpose.LINK, NOW.plusSeconds(30), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42"))
                .thenReturn(Optional.of(UserIdentity.link(UUID.randomUUID(), AuthProvider.GITHUB, "42", NOW)));

        assertLinkRejected(OAuthLoginException.Reason.IDENTITY_ALREADY_LINKED);
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    void linkIsIdempotentWhenTheIdentityAlreadyBelongsToTheUser() {
        stubToken(AuthorizationCodePurpose.LINK, NOW.plusSeconds(30), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42"))
                .thenReturn(Optional.of(UserIdentity.link(user.getId(), AuthProvider.GITHUB, "42", NOW)));

        linkService.execute(new LinkOAuthIdentityCommand("raw", AuthProvider.GITHUB, "42"));

        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    void linkRejectsASecondIdentityOfTheSameProvider() {
        stubToken(AuthorizationCodePurpose.LINK, NOW.plusSeconds(30), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "42")).thenReturn(Optional.empty());
        when(identityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.GITHUB))
                .thenReturn(Optional.of(UserIdentity.link(user.getId(), AuthProvider.GITHUB, "7", NOW)));

        assertLinkRejected(OAuthLoginException.Reason.PROVIDER_ALREADY_LINKED);
    }

    @Test
    void unlinkRemovesTheIdentityWhenThereIsAPassword() {
        UserIdentity identity = UserIdentity.link(user.getId(), AuthProvider.GITHUB, "42", NOW);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByUserId(user.getId())).thenReturn(List.of(identity));

        new UnlinkOAuthIdentityService(userRepository, identityRepository)
                .execute(new UnlinkOAuthIdentityCommand(user.getId(), AuthProvider.GITHUB));

        verify(identityRepository).delete(identity.getId());
    }

    @Test
    void unlinkRefusesToRemoveTheOnlyLoginMethod() {
        User external = User.registerExternal("Octo", "octo@example.com", null);
        UserIdentity identity = UserIdentity.link(external.getId(), AuthProvider.GITHUB, "42", NOW);
        when(userRepository.findById(external.getId())).thenReturn(Optional.of(external));
        when(identityRepository.findByUserId(external.getId())).thenReturn(List.of(identity));

        assertThatThrownBy(() -> new UnlinkOAuthIdentityService(userRepository, identityRepository)
                .execute(new UnlinkOAuthIdentityCommand(external.getId(), AuthProvider.GITHUB)))
                .isInstanceOf(BusinessRuleException.class);
        verify(identityRepository, never()).delete(any(UUID.class));
    }

    @Test
    void unlinkFailsWhenTheIdentityIsNotLinked() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByUserId(user.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> new UnlinkOAuthIdentityService(userRepository, identityRepository)
                .execute(new UnlinkOAuthIdentityCommand(user.getId(), AuthProvider.GITHUB)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getIdentitiesReturnsTheLinkedIdentities() {
        UserIdentity identity = UserIdentity.link(user.getId(), AuthProvider.GITHUB, "42", NOW);
        when(identityRepository.findByUserId(user.getId())).thenReturn(List.of(identity));

        assertThat(new GetUserIdentitiesService(identityRepository).execute(new GetUserIdentitiesQuery(user.getId())))
                .containsExactly(identity);
    }

    private void stubToken(AuthorizationCodePurpose purpose, Instant expiresAt, boolean markUsed) {
        AuthorizationCode token = AuthorizationCode.issue(user.getId(), purpose, "hashed", NOW.minusSeconds(30), expiresAt);
        when(codeCodec.hash("raw")).thenReturn("hashed");
        when(codeRepository.findByCodeHash("hashed")).thenReturn(Optional.of(token));
        if (markUsed) {
            when(codeRepository.markUsed(token.getId(), NOW)).thenReturn(true);
        }
    }

    private void assertLinkRejected(OAuthLoginException.Reason reason) {
        assertThatThrownBy(() -> linkService.execute(new LinkOAuthIdentityCommand("raw", AuthProvider.GITHUB, "42")))
                .isInstanceOf(OAuthLoginException.class)
                .extracting(exception -> ((OAuthLoginException) exception).getReason())
                .isEqualTo(reason);
    }
}
