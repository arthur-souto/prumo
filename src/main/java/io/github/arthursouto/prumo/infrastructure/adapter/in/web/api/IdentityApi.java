package io.github.arthursouto.prumo.infrastructure.adapter.in.web.api;

import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.IdentityResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LinkTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Identities", description = "External login methods linked to the authenticated account")
@RequestMapping("/api/v1/users/me/identities")
public interface IdentityApi {

    @Operation(
            summary = "List linked identities",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Linked identities")
    @GetMapping
    List<IdentityResponse> list(@Parameter(hidden = true) Authentication authentication);

    @Operation(
            summary = "Start linking an external account",
            description = "Returns a path with a one-time link token. Send the browser to it to authorize the"
                    + " provider; the account is linked when the provider redirects back. The redirect to the"
                    + " front end carries linked=<provider> or error=<reason>.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link token created"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
    })
    @PostMapping("/{provider}/link")
    ResponseEntity<LinkTokenResponse> startLink(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable String provider
    );

    @Operation(
            summary = "Unlink an external account",
            description = "Refused when it is the only way to log in to the account.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Identity unlinked"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token"),
            @ApiResponse(responseCode = "404", description = "Identity not linked"),
            @ApiResponse(responseCode = "422", description = "It is the only login method of the account")
    })
    @DeleteMapping("/{provider}")
    ResponseEntity<Void> unlink(
            @Parameter(hidden = true) Authentication authentication,
            @PathVariable String provider
    );
}
