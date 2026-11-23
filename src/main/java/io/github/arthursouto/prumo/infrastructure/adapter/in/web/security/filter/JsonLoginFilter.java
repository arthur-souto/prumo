package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.filter;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LoginRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.exception.InvalidLoginRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

public class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {

    public static final String LOGIN_PATH = "/api/v1/auth/login";

    private final JsonMapper jsonMapper;

    public JsonLoginFilter(AuthenticationManager authenticationManager,
                           JsonMapper jsonMapper,
                           AuthenticationSuccessHandler successHandler,
                           AuthenticationFailureHandler failureHandler) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGIN_PATH), authenticationManager);
        this.jsonMapper = jsonMapper;
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        setSecurityContextRepository(new RequestAttributeSecurityContextRepository());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        LoginRequest body = readBody(request);

        if (body == null || isBlank(body.email()) || isBlank(body.password())) {
            throw new InvalidLoginRequestException();
        }

        return getAuthenticationManager().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.password())
        );
    }

    private LoginRequest readBody(HttpServletRequest request) {
        try {
            return jsonMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException | JacksonException exception) {
            throw new InvalidLoginRequestException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
