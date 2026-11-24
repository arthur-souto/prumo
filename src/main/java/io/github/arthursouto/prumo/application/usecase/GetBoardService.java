package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetBoardUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetBoardQuery;
import io.github.arthursouto.prumo.application.port.in.result.BoardView;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class GetBoardService implements GetBoardUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final StoryRepositoryPort storyRepository;

    public GetBoardService(WorkspaceAccess access, BoardProvisioner boards, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.boards = boards;
        this.storyRepository = storyRepository;
    }

    @Override
    public BoardView execute(GetBoardQuery query) {
        Workspace workspace = access.member(query.userId(), query.workspaceId());

        return new BoardView(boards.ensure(workspace), storyRepository.findAllByWorkspaceId(workspace.getId()));
    }
}
