package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "External login method linked to the account")
public record IdentityResponse(AuthProvider provider, Instant linkedAt) {

    public static IdentityResponse from(UserIdentity identity) {
        return new IdentityResponse(identity.getProvider(), identity.getCreatedAt());
    }
}
