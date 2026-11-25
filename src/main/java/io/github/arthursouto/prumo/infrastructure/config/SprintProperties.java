package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties("app.sprints")
public record SprintProperties(ZoneId timeZone, Snapshots snapshots) {

    public SprintProperties {
        timeZone = timeZone == null ? ZoneId.of("UTC") : timeZone;
    }

    public record Snapshots(boolean enabled, Duration interval, Duration initialDelay) {
    }
}
