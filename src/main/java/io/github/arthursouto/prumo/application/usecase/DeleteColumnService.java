package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DeleteColumnCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;

public class DeleteColumnService implements DeleteColumnUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;
    private final StoryRepositoryPort storyRepository;

    public DeleteColumnService(WorkspaceAccess access,
                               BoardProvisioner boards,
                               BoardRepositoryPort boardRepository,
                               StoryRepositoryPort storyRepository) {
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public void execute(DeleteColumnCommand command) {
        Board board = boards.ensure(access.editor(command.userId(), command.workspaceId()));
        List<UserStory> stories = storyRepository.findAllByColumnId(command.columnId());
        BoardColumn removed = board.remove(command.columnId(), !stories.isEmpty(), command.moveToColumnId());

        if (!stories.isEmpty()) {
            relocate(stories, command.moveToColumnId());
        }

        boardRepository.deleteColumn(removed.getId());
        boardRepository.save(board);
    }

    private void relocate(List<UserStory> stories, java.util.UUID destinationId) {
        List<UserStory> destination = storyRepository.findAllByColumnId(destinationId);
        String last = destination.isEmpty() ? null : destination.get(destination.size() - 1).getRank();

        for (UserStory story : stories) {
            last = Rank.between(last, null);
            story.moveTo(destinationId, last);
        }

        storyRepository.saveAll(stories);
    }
}
