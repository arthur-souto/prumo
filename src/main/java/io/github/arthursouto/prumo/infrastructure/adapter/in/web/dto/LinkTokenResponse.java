package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Where to send the browser to link the external account")
public record LinkTokenResponse(
        @JsonProperty("authorization_path")
        @Schema(example = "/oauth2/authorization/github?link_token=abc")
        String authorizationPath
) {
}
