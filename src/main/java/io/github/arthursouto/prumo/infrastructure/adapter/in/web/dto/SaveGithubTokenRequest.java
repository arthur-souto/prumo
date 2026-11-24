package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A GitHub personal access token with repo scope")
public record SaveGithubTokenRequest(@NotBlank @Size(max = 255) String token) {

    @Override
    public String toString() {
        return "SaveGithubTokenRequest[token=****]";
    }
}
