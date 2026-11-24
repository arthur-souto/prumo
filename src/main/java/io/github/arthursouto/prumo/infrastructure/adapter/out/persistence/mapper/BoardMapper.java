package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.BoardColumnEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.BoardEntity;

import java.util.List;

public final class BoardMapper {

    private BoardMapper() {
    }

    public static BoardColumnEntity toEntity(BoardColumn column) {
        BoardColumnEntity entity = new BoardColumnEntity();
        entity.setId(column.getId());
        entity.setWorkspaceId(column.getWorkspaceId());
        entity.setName(column.getName());
        entity.setKind(column.getKind());
        entity.setPosition(column.getPosition());
        entity.setCreatedAt(column.getCreatedAt());
        return entity;
    }

    public static BoardColumn toDomain(BoardColumnEntity entity) {
        return BoardColumn.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getName(),
                entity.getKind(),
                entity.getPosition(),
                entity.getCreatedAt()
        );
    }

    public static Board toDomain(BoardEntity entity, List<BoardColumnEntity> columns) {
        return Board.of(
                entity.getWorkspaceId(),
                entity.getKeyPrefix(),
                entity.isAutomationEnabled(),
                columns.stream().map(BoardMapper::toDomain).toList()
        );
    }
}
