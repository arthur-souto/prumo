package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.exception.InvalidLoginRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final JsonMapper jsonMapper;

    public LoginFailureHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        HttpStatus status = exception instanceof InvalidLoginRequestException
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.UNAUTHORIZED;
        String detail = status == HttpStatus.BAD_REQUEST ? "Invalid login request" : "Invalid email or password";

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());
        problem.put("status", status.value());
        problem.put("title", status.getReasonPhrase());

        response.setStatus(status.value());
        response.setContentType(PROBLEM_JSON);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
