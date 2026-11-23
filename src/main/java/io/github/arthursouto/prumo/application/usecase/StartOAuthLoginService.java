package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.OAuthLoginException;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.application.port.in.command.StartOAuthLoginCommand;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class StartOAuthLoginService implements StartOAuthLoginUseCase {

    private final UserRepositoryPort userRepository;
    private final UserIdentityRepositoryPort identityRepository;
    private final AuthorizationCodeRepositoryPort codeRepository;
    private final AuthorizationCodeCodecPort codeCodec;
    private final Clock clock;
    private final Duration codeTtl;

    public StartOAuthLoginService(UserRepositoryPort userRepository,
                                  UserIdentityRepositoryPort identityRepository,
                                  AuthorizationCodeRepositoryPort codeRepository,
                                  AuthorizationCodeCodecPort codeCodec,
                                  Clock clock,
                                  Duration codeTtl) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.codeRepository = codeRepository;
        this.codeCodec = codeCodec;
        this.clock = clock;
        this.codeTtl = codeTtl;
    }

    @Override
    public String execute(StartOAuthLoginCommand command) {
        User user = identityRepository.findByProviderAndProviderId(command.provider(), command.providerId())
                .map(identity -> userRepository.findById(identity.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .orElseGet(() -> register(command));

        if (!user.isEnabled()) {
            throw new OAuthLoginException(OAuthLoginException.Reason.ACCOUNT_DISABLED);
        }

        Instant now = clock.instant();
        String rawCode = codeCodec.generate();
        codeRepository.save(AuthorizationCode.issue(
                user.getId(),
                AuthorizationCodePurpose.LOGIN,
                codeCodec.hash(rawCode),
                now,
                now.plus(codeTtl)
        ));

        return rawCode;
    }

    private User register(StartOAuthLoginCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            throw new OAuthLoginException(OAuthLoginException.Reason.EMAIL_UNAVAILABLE);
        }

        String email = command.email().strip().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new OAuthLoginException(OAuthLoginException.Reason.EMAIL_ALREADY_REGISTERED);
        }

        User user = userRepository.save(User.registerExternal(
                command.name().strip(),
                email,
                command.avatarUrl()
        ));
        identityRepository.save(UserIdentity.link(
                user.getId(),
                command.provider(),
                command.providerId(),
                clock.instant()
        ));

        return user;
    }
}
