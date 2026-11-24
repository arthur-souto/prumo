package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.story.SubtaskProgress;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BoardView(Board board, List<UserStory> stories, Map<UUID, SubtaskProgress> subtaskProgress) {

    public BoardView(Board board, List<UserStory> stories) {
        this(board, stories, Map.of());
    }
}
