package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetBoardUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetBoardQuery;
import io.github.arthursouto.prumo.application.port.in.result.BoardView;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import java.util.List;

public class GetBoardService implements GetBoardUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final StoryRepositoryPort storyRepository;
    private final SubtaskRepositoryPort subtaskRepository;

    public GetBoardService(WorkspaceAccess access, BoardProvisioner boards, StoryRepositoryPort storyRepository, SubtaskRepositoryPort subtaskRepository) {
        this.access = access;
        this.boards = boards;
        this.storyRepository = storyRepository;
        this.subtaskRepository = subtaskRepository;
    }

    @Override
    public BoardView execute(GetBoardQuery query) {
        Workspace workspace = access.member(query.userId(), query.workspaceId());

        List<UserStory> stories = storyRepository.findAllByWorkspaceId(workspace.getId());

        return new BoardView(
                boards.ensure(workspace),
                stories,
                subtaskRepository.progressByStoryIds(stories.stream().map(UserStory::getId).toList())
        );
    }
}
