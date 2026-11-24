package io.github.arthursouto.prumo.infrastructure.adapter.out.github;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.result.PullRequestPage;
import io.github.arthursouto.prumo.application.port.out.result.RemotePullRequest;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class GithubApiClient implements GithubPort {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9_.-]{1,100}");
    private static final String API_VERSION = "2022-11-28";

    private final RestClient restClient;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GithubApiClient(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    @Override
    public String authenticate(String token) {
        return text(send(HttpMethod.GET, "/user", token, null, null).body, "login");
    }

    @Override
    public List<RemoteRepository> listRepositories(String token) {
        Response response = send(
                HttpMethod.GET,
                "/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member",
                token,
                null,
                null
        );
        List<RemoteRepository> repositories = new ArrayList<>();

        response.body.forEach(node -> repositories.add(repository(node)));

        return repositories;
    }

    @Override
    public RemoteRepository getRepository(String token, String owner, String name) {
        requireSegments(owner, name);

        return repository(send(HttpMethod.GET, "/repos/" + owner + "/" + name, token, null, null).body);
    }

    @Override
    public RemoteRepository createRepository(String token, String name, String description, boolean isPrivate) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("private", isPrivate);
        body.put("auto_init", true);

        if (description != null) {
            body.put("description", description);
        }

        try {
            return repository(send(HttpMethod.POST, "/user/repos", token, body, null).body);
        } catch (GithubIntegrationException exception) {
            if (exception.getReason() == GithubFailure.PERMISSION_DENIED && exception.getMessage().contains("exists")) {
                throw new GithubIntegrationException(GithubFailure.REPOSITORY_EXISTS, "A repository with this name already exists");
            }

            throw exception;
        }
    }

    @Override
    public void createBranch(String token, String owner, String repository, String branch, String fromBranch) {
        requireSegments(owner, repository);

        String sha = text(
                send(HttpMethod.GET, "/repos/" + owner + "/" + repository + "/git/ref/heads/" + fromBranch, token, null, null)
                        .body.path("object"),
                "sha"
        );

        send(
                HttpMethod.POST,
                "/repos/" + owner + "/" + repository + "/git/refs",
                token,
                Map.of("ref", "refs/heads/" + branch, "sha", sha),
                null
        );
    }

    @Override
    public PullRequestPage listPullRequests(String token, String owner, String repository, String etag) {
        requireSegments(owner, repository);

        Response response = send(
                HttpMethod.GET,
                "/repos/" + owner + "/" + repository + "/pulls?state=all&sort=updated&direction=desc&per_page=50",
                token,
                null,
                etag
        );

        if (response.status == 304) {
            return new PullRequestPage(true, etag, List.of());
        }

        List<RemotePullRequest> items = new ArrayList<>();

        response.body.forEach(node -> items.add(new RemotePullRequest(
                node.path("number").asInt(),
                node.path("title").asString(""),
                state(node),
                node.path("html_url").asString(""),
                node.path("head").path("ref").asString(null),
                Instant.parse(node.path("updated_at").asString())
        )));

        return new PullRequestPage(false, response.etag, items);
    }

    private Response send(HttpMethod method, String path, String token, Object body, String etag) {
        try {
            return restClient.method(method)
                    .uri(path)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                        headers.set("X-GitHub-Api-Version", API_VERSION);

                        if (etag != null) {
                            headers.set(HttpHeaders.IF_NONE_MATCH, etag);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "" : body)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        String raw = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

                        if (status >= 400) {
                            throw failure(status, response.getHeaders(), raw);
                        }

                        JsonNode parsed = status == 304 || raw.isBlank() ? mapper.createObjectNode() : mapper.readTree(raw);

                        return new Response(status, parsed, response.getHeaders().getETag());
                    });
        } catch (GithubIntegrationException exception) {
            throw exception;
        } catch (RestClientException | tools.jackson.core.JacksonException exception) {
            throw new GithubIntegrationException(GithubFailure.UNAVAILABLE, "GitHub could not be reached. Try again shortly");
        }
    }

    private GithubIntegrationException failure(int status, HttpHeaders headers, String raw) {
        String message = message(raw);
        boolean limited = "0".equals(headers.getFirst("X-RateLimit-Remaining"))
                || headers.getFirst(HttpHeaders.RETRY_AFTER) != null
                || message.toLowerCase().contains("rate limit");

        if (status == 401) {
            return new GithubIntegrationException(GithubFailure.INVALID_TOKEN, "The GitHub token is invalid or expired");
        }

        if (status == 429 || (status == 403 && limited)) {
            return new GithubIntegrationException(GithubFailure.RATE_LIMITED, "GitHub rate limit reached. Try again later");
        }

        if (status == 403) {
            return new GithubIntegrationException(GithubFailure.PERMISSION_DENIED, "The GitHub token does not have access to this resource");
        }

        if (status == 404 || status == 409) {
            return new GithubIntegrationException(GithubFailure.NOT_FOUND, "The repository or branch was not found on GitHub");
        }

        if (status == 422 && message.toLowerCase().contains("reference already exists")) {
            return new GithubIntegrationException(GithubFailure.BRANCH_EXISTS, "A branch with this name already exists");
        }

        if (status == 422) {
            return new GithubIntegrationException(GithubFailure.PERMISSION_DENIED, "GitHub rejected the request: " + message);
        }

        return new GithubIntegrationException(GithubFailure.UNAVAILABLE, "GitHub is unavailable. Try again shortly");
    }

    private String message(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            StringBuilder builder = new StringBuilder(node.path("message").asString(""));

            node.path("errors").forEach(error -> builder.append(' ').append(error.path("message").asString("")));

            return builder.toString().strip();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static RemoteRepository repository(JsonNode node) {
        return new RemoteRepository(
                node.path("owner").path("login").asString(""),
                node.path("name").asString(""),
                node.path("default_branch").asString("main"),
                node.path("html_url").asString(""),
                node.path("private").asBoolean(false),
                node.path("description").asString(null)
        );
    }

    private static PullRequestState state(JsonNode node) {
        if (!node.path("merged_at").isNull() && !node.path("merged_at").isMissingNode()) {
            return PullRequestState.MERGED;
        }

        return "open".equals(node.path("state").asString("")) ? PullRequestState.OPEN : PullRequestState.CLOSED;
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asString(null);

        if (value == null || value.isBlank()) {
            throw new GithubIntegrationException(GithubFailure.UNAVAILABLE, "GitHub returned an unexpected response");
        }

        return value;
    }

    private static void requireSegments(String... segments) {
        for (String segment : segments) {
            if (segment == null || !SEGMENT.matcher(segment).matches()) {
                throw new GithubIntegrationException(GithubFailure.NOT_FOUND, "The repository was not found on GitHub");
            }
        }
    }

    private record Response(int status, JsonNode body, String etag) {
    }
}
