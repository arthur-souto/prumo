package io.github.arthursouto.prumo.application.port.out.result;

import java.util.List;

public record PullRequestPage(boolean notModified, String etag, List<RemotePullRequest> items) {
}
