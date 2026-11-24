package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.application.port.out.result.PullRequestPage;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import java.util.List;

public interface GithubPort {

    String authenticate(String token);

    List<RemoteRepository> listRepositories(String token);

    RemoteRepository getRepository(String token, String owner, String name);

    RemoteRepository createRepository(String token, String name, String description, boolean isPrivate);

    void createBranch(String token, String owner, String repository, String branch, String fromBranch);

    PullRequestPage listPullRequests(String token, String owner, String repository, String etag);
}
