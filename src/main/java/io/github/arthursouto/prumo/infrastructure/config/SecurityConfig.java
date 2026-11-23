package io.github.arthursouto.prumo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_ROUTES = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    private static final String[] LOGIN_ROUTES = {
            "/login",
            "/login/**",
            "/oauth2/**"
    };

    private static final String[] ACTUATOR_ROUTES = {
            "/actuator",
            "/actuator/**"
    };

    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/public/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_ROUTES).permitAll()
                        .requestMatchers(LOGIN_ROUTES).permitAll()
                        .requestMatchers(ACTUATOR_ROUTES).permitAll()
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults());
        return http.build();
    }
}
