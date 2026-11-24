package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.activity.Activity;

/** An entry with the names resolved: the actor and, for an assignment, the previous and new assignee. */
public record ActivityView(Activity activity, String actorName, String fromLabel, String toLabel) {
}
