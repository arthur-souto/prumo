package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.github")
public record GithubProperties(String apiUrl, String encryptionKey, Polling polling) {

    public record Polling(boolean enabled, Duration interval) {
    }
}
