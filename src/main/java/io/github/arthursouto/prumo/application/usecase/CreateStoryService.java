package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.List;

public class CreateStoryService implements CreateStoryUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;
    private final StoryRepositoryPort storyRepository;
    private final ActivityLog activity;

    public CreateStoryService(WorkspaceAccess access,
                              BoardProvisioner boards,
                              BoardRepositoryPort boardRepository,
                              StoryRepositoryPort storyRepository,
                              ActivityLog activity) {
        this.activity = activity;
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(CreateStoryCommand command) {
        Workspace workspace = access.member(command.userId(), command.workspaceId());
        Board board = boards.ensure(workspace);
        BoardColumn column = command.columnId() == null
                ? board.ofKind(ColumnKind.TODO).orElseGet(board::first)
                : board.require(command.columnId());
        List<UserStory> inColumn = storyRepository.findAllByColumnId(column.getId());
        String lastRank = inColumn.isEmpty() ? null : inColumn.get(inColumn.size() - 1).getRank();
        int number = boardRepository.allocateStoryNumber(workspace.getId());

        UserStory created = storyRepository.save(UserStory.create(
                workspace.getId(),
                number,
                board.getKeyPrefix() + "-" + number,
                command.title().strip(),
                Checklists.text(command.description()),
                command.priority() == null ? StoryPriority.MEDIUM : command.priority(),
                command.storyPoints(),
                column.getId(),
                Rank.between(lastRank, null),
                Checklists.labels(command.labels()),
                Checklists.items(command.acceptanceCriteria(), List.of())
        ));

        activity.record(workspace.getId(), created.getId(), Actor.user(command.userId()), ActivityType.STORY_CREATED, null, created.getTitle());

        return created;
    }
}
