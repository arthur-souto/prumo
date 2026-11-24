package io.github.arthursouto.prumo.domain.model.story;

import java.time.Instant;
import java.util.UUID;

public record PullRequest(
        UUID id,
        UUID storyId,
        UUID repositoryId,
        int number,
        String title,
        PullRequestState state,
        String url,
        String branch,
        Instant updatedAt
) {

    public static PullRequest track(UUID storyId,
                                    UUID repositoryId,
                                    int number,
                                    String title,
                                    PullRequestState state,
                                    String url,
                                    String branch,
                                    Instant updatedAt) {
        return new PullRequest(UUID.randomUUID(), storyId, repositoryId, number, title, state, url, branch, updatedAt);
    }

    public PullRequest refreshed(UUID storyId,
                                 String title,
                                 PullRequestState state,
                                 String url,
                                 String branch,
                                 Instant updatedAt) {
        return new PullRequest(id, storyId, repositoryId, number, title, state, url, branch, updatedAt);
    }
}
