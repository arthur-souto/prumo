package io.github.arthursouto.prumo.infrastructure.adapter.out.github;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.out.result.PullRequestPage;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubApiClientTest {

    private static final String API = "https://api.github.com";
    private static final String TOKEN = "ghp_supersecret";

    private MockRestServiceServer server;
    private GithubApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GithubApiClient(builder.build());
    }

    private static final String PULLS = API + "/repos/arthur/prumo/pulls?state=all&sort=updated&direction=desc&per_page=50";

    @Test
    void authenticateReturnsTheLoginAndSendsTheBearerTokenAndApiVersion() {
        server.expect(requestTo(API + "/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess("{\"login\":\"arthur\"}", MediaType.APPLICATION_JSON));

        assertThat(client.authenticate(TOKEN)).isEqualTo("arthur");
        server.verify();
    }

    @Test
    void anInvalidTokenIsReportedWithoutLeakingIt() {
        server.expect(requestTo(API + "/user"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Bad credentials\"}"));

        assertThatThrownBy(() -> client.authenticate(TOKEN))
                .isInstanceOfSatisfying(GithubIntegrationException.class, exception -> {
                    assertThat(exception.getReason()).isEqualTo(GithubFailure.INVALID_TOKEN);
                    assertThat(exception.getMessage()).doesNotContain(TOKEN);
                });
    }

    @Test
    void listsRepositoriesMappingOwnerBranchAndVisibility() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(API + "/user/repos")))
                .andRespond(withSuccess("""
                        [{"name":"prumo","owner":{"login":"arthur"},"default_branch":"main",
                          "html_url":"https://github.com/arthur/prumo","private":true,"description":"d"},
                         {"name":"lib","owner":{"login":"acme"},"html_url":"https://github.com/acme/lib","private":false}]
                        """, MediaType.APPLICATION_JSON));

        List<RemoteRepository> repositories = client.listRepositories(TOKEN);

        assertThat(repositories).hasSize(2);
        assertThat(repositories.get(0)).isEqualTo(
                new RemoteRepository("arthur", "prumo", "main", "https://github.com/arthur/prumo", true, "d")
        );
        assertThat(repositories.get(1).defaultBranch()).isEqualTo("main");
        assertThat(repositories.get(1).description()).isNull();
    }

    @Test
    void getsASingleRepository() {
        server.expect(requestTo(API + "/repos/arthur/prumo"))
                .andRespond(withSuccess("""
                        {"name":"prumo","owner":{"login":"arthur"},"default_branch":"develop",
                         "html_url":"https://github.com/arthur/prumo","private":false}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getRepository(TOKEN, "arthur", "prumo").defaultBranch()).isEqualTo("develop");
    }

    @Test
    void aMissingRepositoryIsNotFound() {
        server.expect(requestTo(API + "/repos/arthur/nope")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getRepository(TOKEN, "arthur", "nope"))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.NOT_FOUND));
    }

    @Test
    void unsafeOwnerAndRepositoryNamesNeverReachTheNetwork() {
        assertThatThrownBy(() -> client.getRepository(TOKEN, "../admin", "x"))
                .isInstanceOf(GithubIntegrationException.class);
        assertThatThrownBy(() -> client.listPullRequests(TOKEN, "a", "b/c", null))
                .isInstanceOf(GithubIntegrationException.class);
        assertThatThrownBy(() -> client.getRepository(TOKEN, "a", null))
                .isInstanceOf(GithubIntegrationException.class);
        server.verify();
    }

    @Test
    void createsAnInitialisedRepositoryWithTheRequestedVisibility() {
        server.expect(requestTo(API + "/user/repos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"name\":\"pru-1\",\"private\":true,\"auto_init\":true,\"description\":\"d\"}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"name":"pru-1","owner":{"login":"arthur"},"default_branch":"main",
                         "html_url":"https://github.com/arthur/pru-1","private":true}
                        """));

        assertThat(client.createRepository(TOKEN, "pru-1", "d", true).owner() + "/" + "pru-1").isEqualTo("arthur/pru-1");
    }

    @Test
    void anExistingRepositoryNameIsReportedAsSuch() {
        server.expect(requestTo(API + "/user/repos"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Repository creation failed.\",\"errors\":[{\"message\":\"name already exists on this account\"}]}"));

        assertThatThrownBy(() -> client.createRepository(TOKEN, "pru-1", null, false))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.REPOSITORY_EXISTS));
    }

    @Test
    void createsABranchFromTheShaOfTheBaseBranch() {
        server.expect(requestTo(API + "/repos/arthur/prumo/git/ref/heads/main"))
                .andRespond(withSuccess("{\"object\":{\"sha\":\"abc123\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(API + "/repos/arthur/prumo/git/refs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"ref\":\"refs/heads/feature/PRU-1-x\",\"sha\":\"abc123\"}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        client.createBranch(TOKEN, "arthur", "prumo", "feature/PRU-1-x", "main");

        server.verify();
    }

    @Test
    void anExistingBranchIsReportedAsSuch() {
        server.expect(requestTo(API + "/repos/arthur/prumo/git/ref/heads/main"))
                .andRespond(withSuccess("{\"object\":{\"sha\":\"abc123\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(API + "/repos/arthur/prumo/git/refs"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Reference already exists\"}"));

        assertThatThrownBy(() -> client.createBranch(TOKEN, "arthur", "prumo", "feature/x", "main"))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.BRANCH_EXISTS));
    }

    @Test
    void mapsPullRequestStatesIncludingMergedClosedPullRequests() {
        server.expect(requestTo(PULLS))
                .andExpect(request -> assertThat(request.getHeaders().containsHeader("If-None-Match")).isFalse())
                .andRespond(withSuccess("""
                        [{"number":3,"title":"Open one","state":"open","merged_at":null,"html_url":"https://github.com/arthur/prumo/pull/3",
                          "head":{"ref":"feature/PRU-1-a"},"updated_at":"2026-01-02T10:00:00Z"},
                         {"number":2,"title":"Merged","state":"closed","merged_at":"2026-01-01T10:00:00Z","html_url":"https://github.com/arthur/prumo/pull/2",
                          "head":{"ref":"PRU-2"},"updated_at":"2026-01-01T10:00:00Z"},
                         {"number":1,"title":"Closed","state":"closed","merged_at":null,"html_url":"https://github.com/arthur/prumo/pull/1",
                          "head":{"ref":"x"},"updated_at":"2025-12-31T10:00:00Z"}]
                        """, MediaType.APPLICATION_JSON).header("ETag", "\"etag-9\""));

        PullRequestPage page = client.listPullRequests(TOKEN, "arthur", "prumo", null);

        assertThat(page.notModified()).isFalse();
        assertThat(page.etag()).isEqualTo("\"etag-9\"");
        assertThat(page.items()).extracting(item -> item.state())
                .containsExactly(PullRequestState.OPEN, PullRequestState.MERGED, PullRequestState.CLOSED);
        assertThat(page.items().get(0).branch()).isEqualTo("feature/PRU-1-a");
        assertThat(page.items().get(0).url()).endsWith("/pull/3");
    }

    @Test
    void sendsTheEtagAndHandlesNotModified() {
        server.expect(requestTo(PULLS))
                .andExpect(header("If-None-Match", "\"etag-9\""))
                .andRespond(withStatus(HttpStatus.NOT_MODIFIED));

        PullRequestPage page = client.listPullRequests(TOKEN, "arthur", "prumo", "\"etag-9\"");

        assertThat(page.notModified()).isTrue();
        assertThat(page.items()).isEmpty();
        assertThat(page.etag()).isEqualTo("\"etag-9\"");
    }

    @Test
    void anExhaustedRateLimitIsDistinguishedFromAPermissionProblem() {
        server.expect(requestTo(PULLS))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).header("X-RateLimit-Remaining", "0")
                        .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"API rate limit exceeded\"}"));
        server.expect(requestTo(PULLS))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Resource not accessible by personal access token\"}"));
        server.expect(requestTo(PULLS)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertReason(GithubFailure.RATE_LIMITED);
        assertReason(GithubFailure.PERMISSION_DENIED);
        assertReason(GithubFailure.RATE_LIMITED);
    }

    private void assertReason(GithubFailure reason) {
        assertThatThrownBy(() -> client.listPullRequests(TOKEN, "arthur", "prumo", null))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(reason));
    }

    @Test
    void serverErrorsAndNetworkFailuresBecomeUnavailable() {
        server.expect(requestTo(PULLS)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(requestTo(PULLS)).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertReason(GithubFailure.UNAVAILABLE);
        assertReason(GithubFailure.UNAVAILABLE);
    }

    @Test
    void anUnexpectedBodyBecomesUnavailableInsteadOfACrash() {
        server.expect(requestTo(API + "/user"))
                .andRespond(withSuccess("<html>nope</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.authenticate(TOKEN))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.UNAVAILABLE));
    }
}
