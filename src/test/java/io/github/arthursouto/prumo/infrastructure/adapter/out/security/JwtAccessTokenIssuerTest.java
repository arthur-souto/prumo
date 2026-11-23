package io.github.arthursouto.prumo.infrastructure.adapter.out.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.user.Role;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessTokenIssuerTest {

    private RSAKey rsaKey;
    private NimbusJwtDecoder decoder;
    private JwtAccessTokenIssuer issuer;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyIDFromThumbprint()
                .build();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();

        JwtProperties properties = new JwtProperties(
                "prumo",
                null,
                null,
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        issuer = new JwtAccessTokenIssuer(
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))),
                rsaKey,
                properties,
                Clock.systemUTC()
        );
    }

    @Test
    void issuesASignedTokenWithTheUserClaims() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");

        AccessToken accessToken = issuer.issue(user);
        Jwt jwt = decoder.decode(accessToken.value());

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("prumo");
        assertThat(jwt.<String>getClaim("email")).isEqualTo("arthur@prumo.io");
        assertThat(jwt.<String>getClaim("name")).isEqualTo("Arthur");
        assertThat(jwt.<List<String>>getClaim("roles")).containsExactly("ROLE_USER");
        assertThat(jwt.getId()).isNotBlank();
    }

    @Test
    void usesRs256AndExposesTheKeyId() {
        Jwt jwt = decoder.decode(issuer.issue(User.registerLocal("Arthur", "arthur@prumo.io", "hashed")).value());

        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(jwt.getHeaders().get("kid")).isEqualTo(rsaKey.getKeyID());
    }

    @Test
    void expiresAfterTheConfiguredTimeToLive() {
        AccessToken accessToken = issuer.issue(User.registerLocal("Arthur", "arthur@prumo.io", "hashed"));
        Jwt jwt = decoder.decode(accessToken.value());

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getExpiresAt()).isEqualTo(accessToken.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void reflectsTheAdminRole() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        user.changeRole(Role.ADMIN);

        Jwt jwt = decoder.decode(issuer.issue(user).value());

        assertThat(jwt.<List<String>>getClaim("roles")).containsExactly("ROLE_ADMIN");
    }

    @Test
    void doesNotLeakSensitiveData() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");

        Jwt jwt = decoder.decode(issuer.issue(user).value());

        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash");
    }
}
