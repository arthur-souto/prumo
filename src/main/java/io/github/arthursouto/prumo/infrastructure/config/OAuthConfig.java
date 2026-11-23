package io.github.arthursouto.prumo.infrastructure.config;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth.GithubOAuth2UserService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfig {

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> githubOAuth2UserService(OAuthProperties properties) {
        return new GithubOAuth2UserService(
                new DefaultOAuth2UserService(),
                RestClient.builder().baseUrl(properties.githubApiUrl()).build()
        );
    }
}
