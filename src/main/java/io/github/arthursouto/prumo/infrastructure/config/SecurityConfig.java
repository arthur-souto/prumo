package io.github.arthursouto.prumo.infrastructure.config;

import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.StartOAuthLoginUseCase;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.filter.JsonLoginFilter;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.LoginFailureHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.LoginSuccessHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.OAuthLoginFailureHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler.OAuthLoginSuccessHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth.LinkTokenAuthorizationRequestResolver;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.provider.LocalAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

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

    private static final String[] AUTH_ROUTES = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/oauth/exchange",
            "/.well-known/jwks.json"
    };

    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/public/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticateUserUseCase authenticateUser,
                                            IssueTokensUseCase issueTokens,
                                            StartOAuthLoginUseCase startOAuthLogin,
                                            LinkOAuthIdentityUseCase linkOAuthIdentity,
                                            ClientRegistrationRepository clientRegistrations,
                                            OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
                                            OAuthProperties oauthProperties,
                                            JsonMapper jsonMapper,
                                            Clock clock) {
        AuthenticationManager authenticationManager = new ProviderManager(
                new LocalAuthenticationProvider(authenticateUser)
        );
        JsonLoginFilter loginFilter = new JsonLoginFilter(
                authenticationManager,
                jsonMapper,
                new LoginSuccessHandler(jsonMapper, issueTokens, clock),
                new LoginFailureHandler(jsonMapper)
        );

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        http
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_ROUTES).permitAll()
                        .requestMatchers(LOGIN_ROUTES).permitAll()
                        .requestMatchers(ACTUATOR_ROUTES).permitAll()
                        .requestMatchers(AUTH_ROUTES).permitAll()
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                ))
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(
                                new LinkTokenAuthorizationRequestResolver(clientRegistrations, "/oauth2/authorization")
                        ))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler(new OAuthLoginSuccessHandler(
                                startOAuthLogin,
                                linkOAuthIdentity,
                                oauthProperties.frontendCallbackUrl()
                        ))
                        .failureHandler(new OAuthLoginFailureHandler(oauthProperties.frontendCallbackUrl())));
        return http.build();
    }
}
