package io.github.arthursouto.prumo.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RSAKey rsaKey(JwtProperties properties) {
        RSAPublicKey publicKey = read(properties.publicKey(), "public");
        RSAPrivateKey privateKey = read(properties.privateKey(), "private");

        try {
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the JWT signing key", exception);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, JwtProperties properties) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer())
        ));
        return decoder;
    }

    @Bean
    JWKSet publicJwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(Resource resource, String kind) {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "JWT " + kind + " key not found. Generate the key pair as described in the README"
                            + " and set JWT_PRIVATE_KEY_PATH and JWT_PUBLIC_KEY_PATH."
            );
        }

        try (InputStream input = resource.getInputStream()) {
            return "public".equals(kind)
                    ? (T) RsaKeyConverters.x509().convert(input)
                    : (T) RsaKeyConverters.pkcs8().convert(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the JWT " + kind + " key", exception);
        }
    }
}
