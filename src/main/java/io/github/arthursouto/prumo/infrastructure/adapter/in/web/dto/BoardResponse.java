package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "The Kanban board of a workspace. Stories come ordered by rank inside each column")
public record BoardResponse(
        String keyPrefix,
        boolean automationEnabled,
        List<ColumnResponse> columns,
        List<StoryResponse> stories
) {

    public static BoardResponse from(Board board, List<UserStory> stories) {
        return new BoardResponse(
                board.getKeyPrefix(),
                board.isAutomationEnabled(),
                board.getColumns().stream().map(ColumnResponse::from).toList(),
                stories.stream().map(StoryResponse::from).toList()
        );
    }

    public record ColumnResponse(UUID id, String name, ColumnKind kind, int position) {

        public static ColumnResponse from(BoardColumn column) {
            return new ColumnResponse(column.getId(), column.getName(), column.getKind(), column.getPosition());
        }
    }
}
