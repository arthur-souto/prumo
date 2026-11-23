package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidAuthorizationCodeException;
import io.github.arthursouto.prumo.application.port.in.ExchangeOAuthCodeUseCase;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ExchangeOAuthCodeCommand;
import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Clock;
import java.time.Instant;

public class ExchangeOAuthCodeService implements ExchangeOAuthCodeUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthorizationCodeRepositoryPort codeRepository;
    private final AuthorizationCodeCodecPort codeCodec;
    private final Clock clock;
    private final IssueTokensUseCase issueTokens;

    public ExchangeOAuthCodeService(UserRepositoryPort userRepository,
                                    AuthorizationCodeRepositoryPort codeRepository,
                                    AuthorizationCodeCodecPort codeCodec,
                                    Clock clock,
                                    IssueTokensUseCase issueTokens) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.codeCodec = codeCodec;
        this.clock = clock;
        this.issueTokens = issueTokens;
    }

    @Override
    public AuthTokens execute(ExchangeOAuthCodeCommand command) {
        if (command.code() == null || command.code().isBlank()) {
            throw new InvalidAuthorizationCodeException();
        }

        AuthorizationCode code = codeRepository
                .findByCodeHash(codeCodec.hash(command.code()))
                .filter(found -> found.getPurpose() == AuthorizationCodePurpose.LOGIN)
                .orElseThrow(InvalidAuthorizationCodeException::new);

        Instant now = clock.instant();

        if (code.isUsed() || code.isExpired(now) || !codeRepository.markUsed(code.getId(), now)) {
            throw new InvalidAuthorizationCodeException();
        }

        User user = userRepository.findById(code.getUserId()).orElseThrow(InvalidAuthorizationCodeException::new);

        if (!user.isEnabled()) {
            throw new InvalidAuthorizationCodeException();
        }

        return issueTokens.execute(new IssueTokensCommand(user.getId()));
    }
}
