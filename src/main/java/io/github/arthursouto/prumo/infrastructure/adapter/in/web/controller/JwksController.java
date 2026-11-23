package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication")
@RestController
public class JwksController {

    private final JWKSet publicJwkSet;

    public JwksController(JWKSet publicJwkSet) {
        this.publicJwkSet = publicJwkSet;
    }

    @Operation(
            summary = "Public signing keys",
            description = "JSON Web Key Set used by other services to validate access tokens."
    )
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return publicJwkSet.toJSONObject();
    }
}
