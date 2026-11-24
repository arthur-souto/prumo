package io.github.arthursouto.prumo.application.port.out.result;

import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import java.time.Instant;

public record RemotePullRequest(
        int number,
        String title,
        PullRequestState state,
        String url,
        String branch,
        Instant updatedAt
) {
}
