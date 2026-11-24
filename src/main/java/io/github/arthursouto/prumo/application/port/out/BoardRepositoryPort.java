package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.board.Board;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepositoryPort {

    Optional<Board> findByWorkspaceId(UUID workspaceId);

    Board save(Board board);

    boolean existsByKeyPrefix(String keyPrefix);

    int allocateStoryNumber(UUID workspaceId);

    void deleteColumn(UUID columnId);
}
