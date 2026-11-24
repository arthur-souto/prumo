package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.exceptions.BoardAlreadyExistsException;
import io.github.arthursouto.prumo.application.exceptions.KeyPrefixTakenException;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.BoardEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.BoardColumnJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.BoardJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.BoardMapper;
import jakarta.persistence.EntityManager;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class BoardPersistenceAdapter implements BoardRepositoryPort {

    private static final String PREFIX_CONSTRAINT = "uq_boards_key_prefix";
    private static final String PRIMARY_KEY_CONSTRAINT = "pk_boards";

    private final BoardJpaRepository boardRepository;
    private final BoardColumnJpaRepository columnRepository;
    private final EntityManager entityManager;

    public BoardPersistenceAdapter(BoardJpaRepository boardRepository,
                                   BoardColumnJpaRepository columnRepository,
                                   EntityManager entityManager) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Board> findByWorkspaceId(UUID workspaceId) {
        return boardRepository.findById(workspaceId).map(this::load);
    }

    @Override
    @Transactional
    public Board save(Board board) {
        BoardEntity entity = boardRepository.findById(board.getWorkspaceId()).orElseGet(() -> {
            BoardEntity created = new BoardEntity();
            created.setWorkspaceId(board.getWorkspaceId());
            created.setNextStoryNumber(1);
            return created;
        });
        entity.setKeyPrefix(board.getKeyPrefix());
        entity.setAutomationEnabled(board.isAutomationEnabled());
        entity.setConsolidateStories(board.isConsolidateStories());

        try {
            boardRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());

            if (cause.contains(PREFIX_CONSTRAINT)) {
                throw new KeyPrefixTakenException();
            }

            if (cause.contains(PRIMARY_KEY_CONSTRAINT)) {
                throw new BoardAlreadyExistsException();
            }

            throw exception;
        }

        columnRepository.saveAll(board.getColumns().stream().map(BoardMapper::toEntity).toList());

        return load(entity);
    }

    @Override
    public boolean existsByKeyPrefix(String keyPrefix) {
        return boardRepository.existsByKeyPrefix(keyPrefix);
    }

    @Override
    @Transactional
    public int allocateStoryNumber(UUID workspaceId) {
        Object allocated = entityManager.createNativeQuery(
                        "UPDATE boards SET next_story_number = next_story_number + 1 "
                                + "WHERE workspace_id = :workspaceId RETURNING next_story_number - 1"
                )
                .setParameter("workspaceId", workspaceId)
                .getSingleResult();

        return ((Number) allocated).intValue();
    }

    @Override
    @Transactional
    public void deleteColumn(UUID columnId) {
        columnRepository.deleteById(columnId);
        columnRepository.flush();
    }

    private Board load(BoardEntity entity) {
        return BoardMapper.toDomain(entity, columnRepository.findByWorkspaceIdOrderByPosition(entity.getWorkspaceId()));
    }
}
