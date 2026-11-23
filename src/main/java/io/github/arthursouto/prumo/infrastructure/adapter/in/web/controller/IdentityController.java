package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateLinkTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.GetUserIdentitiesUseCase;
import io.github.arthursouto.prumo.application.port.in.UnlinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateLinkTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetUserIdentitiesQuery;
import io.github.arthursouto.prumo.application.port.in.command.UnlinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.IdentityApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.IdentityResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LinkTokenResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
public class IdentityController implements IdentityApi {

    private final GetUserIdentitiesUseCase getUserIdentities;
    private final CreateLinkTokenUseCase createLinkToken;
    private final UnlinkOAuthIdentityUseCase unlinkOAuthIdentity;

    public IdentityController(GetUserIdentitiesUseCase getUserIdentities,
                              CreateLinkTokenUseCase createLinkToken,
                              UnlinkOAuthIdentityUseCase unlinkOAuthIdentity) {
        this.getUserIdentities = getUserIdentities;
        this.createLinkToken = createLinkToken;
        this.unlinkOAuthIdentity = unlinkOAuthIdentity;
    }

    @Override
    public List<IdentityResponse> list(Authentication authentication) {
        return getUserIdentities.execute(new GetUserIdentitiesQuery(userId(authentication)))
                .stream()
                .map(IdentityResponse::from)
                .toList();
    }

    @Override
    public ResponseEntity<LinkTokenResponse> startLink(Authentication authentication, String provider) {
        AuthProvider authProvider = parse(provider);
        String token = createLinkToken.execute(new CreateLinkTokenCommand(userId(authentication)));
        String path = UriComponentsBuilder
                .fromPath("/oauth2/authorization/{provider}")
                .queryParam("link_token", token)
                .buildAndExpand(authProvider.name().toLowerCase(Locale.ROOT))
                .toUriString();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LinkTokenResponse(path));
    }

    @Override
    public ResponseEntity<Void> unlink(Authentication authentication, String provider) {
        unlinkOAuthIdentity.execute(new UnlinkOAuthIdentityCommand(userId(authentication), parse(provider)));

        return ResponseEntity.noContent().build();
    }

    private static AuthProvider parse(String provider) {
        return Arrays.stream(AuthProvider.values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Unknown provider"));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
