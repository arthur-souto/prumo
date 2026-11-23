package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.handler;

import io.github.arthursouto.prumo.application.port.in.IssueTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.TokenResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;

public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JsonMapper jsonMapper;
    private final IssueTokensUseCase issueTokens;
    private final Clock clock;

    public LoginSuccessHandler(JsonMapper jsonMapper, IssueTokensUseCase issueTokens, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.issueTokens = issueTokens;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        AuthTokens tokens = issueTokens.execute(new IssueTokensCommand(principal.id()));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        jsonMapper.writeValue(response.getOutputStream(), TokenResponse.from(tokens, clock.instant()));
    }
}
