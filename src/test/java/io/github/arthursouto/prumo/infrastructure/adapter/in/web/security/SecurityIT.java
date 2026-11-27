package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth.LinkTokenAuthorizationRequestResolver;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with the real security chain, which the controller tests skip: who reaches the API without a
 * token, which tokens are refused, which routes stay public and how workspace roles are enforced from the HTTP request
 * down to PostgreSQL. The JWT key pair is generated here, since the one in {@code keys/} is not committed.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final String PASSWORD = "s3cret-pass";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Path keys = writeKeyPair();
        registry.add("app.security.jwt.private-key", () -> "file:" + keys.resolve("private.pem"));
        registry.add("app.security.jwt.public-key", () -> "file:" + keys.resolve("public.pem"));
        registry.add("spring.security.oauth2.client.registration.github.client-id", () -> "test-client");
        registry.add("spring.security.oauth2.client.registration.github.client-secret", () -> "test-secret");
        registry.add("app.github.polling.enabled", () -> "false");
        registry.add("app.sprints.snapshots.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Path writeKeyPair() {
        KeyPair pair = rsaKeyPair();

        try {
            Path dir = Files.createTempDirectory("prumo-jwt");
            Files.writeString(dir.resolve("private.pem"), pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
            Files.writeString(dir.resolve("public.pem"), pem("PUBLIC KEY", pair.getPublic().getEncoded()));
            dir.toFile().deleteOnExit();
            return dir;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    private record Account(UUID id, String token) {
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Registers and logs in through the real JSON login filter. */
    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@test.dev";
        JsonNode user = body(mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"U\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode tokens = body(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn());

        return new Account(UUID.fromString(user.get("id").asString()), tokens.get("access_token").asString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID workspaceOf(Account owner) throws Exception {
        JsonNode created = body(mockMvc.perform(post("/api/v1/workspaces").header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Team\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        return UUID.fromString(created.get("id").asString());
    }

    private void join(UUID workspaceId, Account account, String role) {
        jdbc.update("INSERT INTO workspace_members (id, workspace_id, user_id, role, created_at) VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), workspaceId, account.id(), role);
    }

    private String token(JwtEncoder signer, String issuer, Instant expiresAt, UUID subject) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject.toString())
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    @Test
    void theApiAnswers401WithoutAToken() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/workspaces").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokensThatAreMalformedForeignExpiredOrFromAnotherIssuerAreRefused() throws Exception {
        UUID userId = account().id();
        KeyPair foreignPair = rsaKeyPair();
        JwtEncoder foreign = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
                new RSAKey.Builder((RSAPublicKey) foreignPair.getPublic()).privateKey(foreignPair.getPrivate()).build()
        )));
        Instant later = Instant.now().plusSeconds(600);

        for (String token : List.of(
                "not-a-jwt",
                token(foreign, "prumo", later, userId),
                token(encoder, "prumo", Instant.now().minusSeconds(120), userId),
                token(encoder, "someone-else", later, userId)
        )) {
            mockMvc.perform(get("/api/v1/workspaces").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get("/api/v1/workspaces").header(HttpHeaders.AUTHORIZATION, bearer(token(encoder, "prumo", later, userId))))
                .andExpect(status().isOk());
    }

    @Test
    void wrongCredentialsAreRefusedByTheLoginFilter() throws Exception {
        account();

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.dev\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void thePublicRoutesNeedNoToken() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        // the login endpoint is a filter, so it only shows in the docs because of LoginOpenApiCustomizer
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.tags", hasItem("Authentication")))
                .andExpect(jsonPath("$.components.schemas.LoginRequest").exists());
    }

    @Test
    void strangersDoNotEvenSeeThatAWorkspaceExists() throws Exception {
        Account owner = account();
        Account stranger = account();
        UUID workspaceId = workspaceOf(owner);
        String auth = bearer(stranger.token());

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/members").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId + "/board").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/workspaces").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void membersReadTheWorkspaceButOnlyOwnersAndAdminsChangeIt() throws Exception {
        Account owner = account();
        Account member = account();
        Account admin = account();
        UUID workspaceId = workspaceOf(owner);
        join(workspaceId, member, "MEMBER");
        join(workspaceId, admin, "ADMIN");
        String path = "/api/v1/workspaces/" + workspaceId;
        String update = "{\"name\":\"Renamed\"}";

        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(member.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get(path + "/members").header(HttpHeaders.AUTHORIZATION, bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.id == '" + owner.id() + "')].role").value(hasItem("OWNER")))
                .andExpect(jsonPath("$[?(@.id == '" + member.id() + "')].role").value(hasItem("MEMBER")));

        mockMvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, bearer(member.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(path + "/board/settings").header(HttpHeaders.AUTHORIZATION, bearer(member.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"automationEnabled\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, bearer(admin.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void onlyTheOwnerDeletesTheWorkspace() throws Exception {
        Account owner = account();
        Account admin = account();
        UUID workspaceId = workspaceOf(owner);
        join(workspaceId, admin, "ADMIN");
        String path = "/api/v1/workspaces/" + workspaceId;

        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(admin.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void startingTheGithubLoginRemembersTheLinkTokenAndForgetsItOnAPlainLogin() throws Exception {
        String attribute = LinkTokenAuthorizationRequestResolver.LINK_TOKEN_SESSION_ATTRIBUTE;

        MvcResult linking = mockMvc.perform(get("/oauth2/authorization/github")
                        .param(LinkTokenAuthorizationRequestResolver.LINK_TOKEN_PARAMETER, "link-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("https://github.com/login/oauth/authorize")))
                .andReturn();
        HttpSession session = linking.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(attribute)).isEqualTo("link-123");

        mockMvc.perform(get("/oauth2/authorization/github").session((MockHttpSession) session))
                .andExpect(status().is3xxRedirection());
        assertThat(session.getAttribute(attribute)).isNull();
    }
}
