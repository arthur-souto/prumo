package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import java.util.List;

public record BoardView(Board board, List<UserStory> stories) {
}
