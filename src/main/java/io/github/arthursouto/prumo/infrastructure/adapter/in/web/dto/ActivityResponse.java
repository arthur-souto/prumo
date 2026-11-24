package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.ActivityView;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "One entry of the history of a story or subtask")
public record ActivityResponse(
        UUID id,
        ActivityType type,
        ChangeSource source,
        @Schema(description = "Null when GitHub made the change") UUID actorId,
        @Schema(description = "Null when GitHub made the change or the user no longer exists") String actorName,
        @Schema(description = "Set when the entry is about a subtask") String subtaskKey,
        @Schema(description = "Previous value: a column name, a status, or an assignee name") String from,
        @Schema(description = "New value: a column name, a status, an assignee name, the title of a new story or subtask, or the changed fields of an update") String to,
        Instant createdAt
) {

    public static ActivityResponse from(ActivityView view) {
        Activity activity = view.activity();

        return new ActivityResponse(
                activity.id(),
                activity.type(),
                activity.source(),
                activity.actorId(),
                view.actorName(),
                activity.subtaskKey(),
                view.fromLabel(),
                view.toLabel(),
                activity.createdAt()
        );
    }
}
