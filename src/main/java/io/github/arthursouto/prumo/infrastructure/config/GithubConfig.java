package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(GithubProperties.class)
public class GithubConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Bean
    RestClient githubRestClient(GithubProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
        );
        requestFactory.setReadTimeout(TIMEOUT);

        return RestClient.builder()
                .baseUrl(properties.apiUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
