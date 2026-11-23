package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

@Schema(description = "Tokens issued on login and refresh")
public record TokenResponse(
        @JsonProperty("access_token")
        @Schema(name = "access_token", description = "JWT sent as Authorization: Bearer")
        String accessToken,
        @JsonProperty("expires_in")
        @Schema(name = "expires_in", description = "Access token lifetime in seconds", example = "900")
        long expiresIn,
        @JsonProperty("refresh_token")
        @Schema(name = "refresh_token", description = "Opaque token used to obtain a new access token")
        String refreshToken
) {

    public static TokenResponse from(AuthTokens tokens, Instant now) {
        return new TokenResponse(
                tokens.accessToken().value(),
                Duration.between(now, tokens.accessToken().expiresAt()).getSeconds(),
                tokens.refreshToken()
        );
    }
}
