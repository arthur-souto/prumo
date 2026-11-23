package io.github.arthursouto.prumo.infrastructure.config;

import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.ExchangeOAuthCodeUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateLinkTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.GetCurrentUserUseCase;
import io.github.arthursouto.prumo.application.port.in.GetUserIdentitiesUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.UnlinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RefreshTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.RegisterUserUseCase;
import io.github.arthursouto.prumo.application.port.in.RevokeRefreshTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeCodecPort;
import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.PasswordHasherPort;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.application.usecase.AuthenticateUserService;
import io.github.arthursouto.prumo.application.usecase.ExchangeOAuthCodeService;
import io.github.arthursouto.prumo.application.usecase.CreateLinkTokenService;
import io.github.arthursouto.prumo.application.usecase.GetCurrentUserService;
import io.github.arthursouto.prumo.application.usecase.GetUserIdentitiesService;
import io.github.arthursouto.prumo.application.usecase.LinkOAuthIdentityService;
import io.github.arthursouto.prumo.application.usecase.UnlinkOAuthIdentityService;
import io.github.arthursouto.prumo.application.usecase.IssueTokensService;
import io.github.arthursouto.prumo.application.usecase.RefreshTokensService;
import io.github.arthursouto.prumo.application.usecase.RegisterUserService;
import io.github.arthursouto.prumo.application.usecase.RevokeRefreshTokenService;
import io.github.arthursouto.prumo.application.usecase.StartOAuthLoginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        return new RegisterUserService(userRepository, passwordHasher);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort userRepository,
                                                    PasswordHasherPort passwordHasher) {
        return new AuthenticateUserService(userRepository, passwordHasher);
    }

    @Bean
    StartOAuthLoginUseCase startOAuthLoginUseCase(UserRepositoryPort userRepository,
                                                  UserIdentityRepositoryPort identityRepository,
                                                  AuthorizationCodeRepositoryPort codeRepository,
                                                  AuthorizationCodeCodecPort codeCodec,
                                                  Clock clock,
                                                  OAuthProperties properties) {
        return new StartOAuthLoginService(
                userRepository,
                identityRepository,
                codeRepository, codeCodec, clock, properties.codeTtl());
    }

    @Bean
    ExchangeOAuthCodeUseCase exchangeOAuthCodeUseCase(UserRepositoryPort userRepository,
                                                      AuthorizationCodeRepositoryPort codeRepository,
                                                      AuthorizationCodeCodecPort codeCodec,
                                                      Clock clock,
                                                      IssueTokensUseCase issueTokens) {
        return new ExchangeOAuthCodeService(userRepository, codeRepository, codeCodec, clock, issueTokens);
    }

    @Bean
    CreateLinkTokenUseCase createLinkTokenUseCase(UserRepositoryPort userRepository,
                                                  AuthorizationCodeRepositoryPort codeRepository,
                                                  AuthorizationCodeCodecPort codeCodec,
                                                  Clock clock,
                                                  OAuthProperties properties) {
        return new CreateLinkTokenService(userRepository, codeRepository, codeCodec, clock, properties.codeTtl());
    }

    @Bean
    LinkOAuthIdentityUseCase linkOAuthIdentityUseCase(UserRepositoryPort userRepository,
                                                      UserIdentityRepositoryPort identityRepository,
                                                      AuthorizationCodeRepositoryPort codeRepository,
                                                      AuthorizationCodeCodecPort codeCodec,
                                                      Clock clock) {
        return new LinkOAuthIdentityService(userRepository, identityRepository, codeRepository, codeCodec, clock);
    }

    @Bean
    UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase(UserRepositoryPort userRepository,
                                                          UserIdentityRepositoryPort identityRepository) {
        return new UnlinkOAuthIdentityService(userRepository, identityRepository);
    }

    @Bean
    GetUserIdentitiesUseCase getUserIdentitiesUseCase(UserIdentityRepositoryPort identityRepository) {
        return new GetUserIdentitiesService(identityRepository);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepositoryPort userRepository) {
        return new GetCurrentUserService(userRepository);
    }

    @Bean
    IssueTokensUseCase issueTokensUseCase(UserRepositoryPort userRepository,
                                          RefreshTokenRepositoryPort refreshTokenRepository,
                                          AccessTokenIssuerPort accessTokenIssuer,
                                          RefreshTokenCodecPort refreshTokenCodec,
                                          Clock clock,
                                          JwtProperties properties) {
        return new IssueTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                clock,
                properties.refreshTokenTtl()
        );
    }

    @Bean
    RefreshTokensUseCase refreshTokensUseCase(UserRepositoryPort userRepository,
                                              RefreshTokenRepositoryPort refreshTokenRepository,
                                              AccessTokenIssuerPort accessTokenIssuer,
                                              RefreshTokenCodecPort refreshTokenCodec,
                                              Clock clock,
                                              JwtProperties properties) {
        return new RefreshTokensService(
                userRepository,
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenCodec,
                clock,
                properties.refreshTokenTtl()
        );
    }

    @Bean
    RevokeRefreshTokenUseCase revokeRefreshTokenUseCase(RefreshTokenRepositoryPort refreshTokenRepository,
                                                        RefreshTokenCodecPort refreshTokenCodec,
                                                        Clock clock) {
        return new RevokeRefreshTokenService(refreshTokenRepository, refreshTokenCodec, clock);
    }
}
