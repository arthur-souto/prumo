package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

public class IssueTokensService implements IssueTokensUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TokenIssuer tokenIssuer;

    public IssueTokensService(UserRepositoryPort userRepository,
                              RefreshTokenRepositoryPort refreshTokenRepository,
                              AccessTokenIssuerPort accessTokenIssuer,
                              RefreshTokenCodecPort refreshTokenCodec,
                              Clock clock,
                              Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenIssuer = new TokenIssuer(accessTokenIssuer, refreshTokenCodec, clock, refreshTokenTtl);
    }

    @Override
    public AuthTokens execute(IssueTokensCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TokenIssuer.IssuedPair issued = tokenIssuer.issue(user, UUID.randomUUID());
        refreshTokenRepository.save(issued.refreshToken());

        return issued.tokens();
    }
}
