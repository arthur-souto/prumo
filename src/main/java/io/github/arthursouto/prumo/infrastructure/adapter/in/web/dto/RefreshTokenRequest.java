package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token issued on login or on a previous refresh")
public record RefreshTokenRequest(
        @JsonProperty("refresh_token")
        @Schema(name = "refresh_token")
        @NotBlank String refreshToken
) {
}
