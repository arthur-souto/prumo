package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.LinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class LinkOAuthIdentityService implements LinkOAuthIdentityUseCase {

    private final UserRepositoryPort userRepository;
    private final UserIdentityRepositoryPort identityRepository;
    private final AuthorizationCodeRepositoryPort codeRepository;
    private final AuthorizationCodeCodecPort codeCodec;
    private final Clock clock;

    public LinkOAuthIdentityService(UserRepositoryPort userRepository,
                                    UserIdentityRepositoryPort identityRepository,
                                    AuthorizationCodeRepositoryPort codeRepository,
                                    AuthorizationCodeCodecPort codeCodec,
                                    Clock clock) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.codeRepository = codeRepository;
        this.codeCodec = codeCodec;
        this.clock = clock;
    }

    @Override
    public void execute(LinkOAuthIdentityCommand command) {
        if (command.linkToken() == null || command.linkToken().isBlank()) {
            throw rejection(OAuthLoginException.Reason.LINK_TOKEN_INVALID);
        }

        AuthorizationCode token = codeRepository
                .findByCodeHash(codeCodec.hash(command.linkToken()))
                .filter(found -> found.getPurpose() == AuthorizationCodePurpose.LINK)
                .orElseThrow(() -> rejection(OAuthLoginException.Reason.LINK_TOKEN_INVALID));

        Instant now = clock.instant();

        if (token.isUsed() || token.isExpired(now) || !codeRepository.markUsed(token.getId(), now)) {
            throw rejection(OAuthLoginException.Reason.LINK_TOKEN_INVALID);
        }

        User user = userRepository.findById(token.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> rejection(OAuthLoginException.Reason.LINK_TOKEN_INVALID));

        Optional<UserIdentity> existing = identityRepository
                .findByProviderAndProviderId(command.provider(), command.providerId());

        if (existing.isPresent()) {
            if (existing.get().getUserId().equals(user.getId())) {
                return;
            }
            throw rejection(OAuthLoginException.Reason.IDENTITY_ALREADY_LINKED);
        }

        if (identityRepository.findByUserIdAndProvider(user.getId(), command.provider()).isPresent()) {
            throw rejection(OAuthLoginException.Reason.PROVIDER_ALREADY_LINKED);
        }

        identityRepository.save(UserIdentity.link(user.getId(), command.provider(), command.providerId(), now));
    }

    private static OAuthLoginException rejection(OAuthLoginException.Reason reason) {
        return new OAuthLoginException(reason);
    }
}
