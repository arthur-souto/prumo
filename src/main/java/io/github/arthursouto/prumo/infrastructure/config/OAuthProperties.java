package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.oauth")
public record OAuthProperties(String frontendCallbackUrl, Duration codeTtl, String githubApiUrl) {
}
