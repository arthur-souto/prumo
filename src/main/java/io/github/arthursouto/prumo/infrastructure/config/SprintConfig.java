package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SprintProperties.class)
public class SprintConfig {
}
