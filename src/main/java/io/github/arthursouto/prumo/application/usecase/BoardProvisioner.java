package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.BoardAlreadyExistsException;
import io.github.arthursouto.prumo.application.exceptions.KeyPrefixTakenException;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.KeyPrefixes;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class BoardProvisioner {

    private static final int MAX_ATTEMPTS = 5;

    private final BoardRepositoryPort boardRepository;

    public BoardProvisioner(BoardRepositoryPort boardRepository) {
        this.boardRepository = boardRepository;
    }

    public Board ensure(Workspace workspace) {
        return boardRepository.findByWorkspaceId(workspace.getId()).orElseGet(() -> create(workspace));
    }

    private Board create(Workspace workspace) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return boardRepository.save(Board.withDefaults(workspace.getId(), availablePrefix(workspace)));
            } catch (KeyPrefixTakenException ignored) {
            } catch (BoardAlreadyExistsException exception) {
                return boardRepository.findByWorkspaceId(workspace.getId()).orElseThrow(() -> exception);
            }
        }

        throw new ConflictException("Could not generate a unique story key prefix");
    }

    private String availablePrefix(Workspace workspace) {
        String base = KeyPrefixes.derive(workspace.getName());
        String prefix = base;
        int suffix = 2;

        while (boardRepository.existsByKeyPrefix(prefix)) {
            prefix = base + suffix++;
        }

        return prefix;
    }
}
