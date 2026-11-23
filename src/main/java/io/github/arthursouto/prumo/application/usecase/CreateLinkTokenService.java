package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateLinkTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateLinkTokenCommand;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class CreateLinkTokenService implements CreateLinkTokenUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthorizationCodeRepositoryPort codeRepository;
    private final AuthorizationCodeCodecPort codeCodec;
    private final Clock clock;
    private final Duration tokenTtl;

    public CreateLinkTokenService(UserRepositoryPort userRepository,
                                  AuthorizationCodeRepositoryPort codeRepository,
                                  AuthorizationCodeCodecPort codeCodec,
                                  Clock clock,
                                  Duration tokenTtl) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.codeCodec = codeCodec;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Override
    public String execute(CreateLinkTokenCommand command) {
        User user = userRepository.findById(command.userId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instant now = clock.instant();
        String rawToken = codeCodec.generate();
        codeRepository.save(AuthorizationCode.issue(
                user.getId(),
                AuthorizationCodePurpose.LINK,
                codeCodec.hash(rawToken),
                now,
                now.plus(tokenTtl)
        ));

        return rawToken;
    }
}
