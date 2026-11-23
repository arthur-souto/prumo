package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "One-time code received in the redirect after a successful OAuth login")
public record ExchangeCodeRequest(@NotBlank String code) {
}
