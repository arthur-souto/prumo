package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.Subtask;

import java.util.List;

/** A subtask with the key of its story and the pull requests that cite it. */
public record SubtaskView(Subtask subtask, String storyKey, List<PullRequest> pullRequests) {
}
