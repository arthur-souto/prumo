package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        String issuer,
        Resource privateKey,
        Resource publicKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
