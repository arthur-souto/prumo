package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.MoveStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.MoveStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MoveStoryService implements MoveStoryUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;
    private final ActivityLog activity;

    public MoveStoryService(WorkspaceAccess access,
                            BoardProvisioner boards,
                            StoryLookup stories,
                            StoryRepositoryPort storyRepository,
                            ActivityLog activity) {
        this.activity = activity;
        this.access = access;
        this.boards = boards;
        this.stories = stories;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(MoveStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());
        Board board = boards.ensure(access.member(command.userId(), command.workspaceId()));
        board.require(command.columnId());
        UUID fromColumn = story.getColumnId();

        UserStory moved = place(command, story, board);

        if (!fromColumn.equals(moved.getColumnId())) {
            activity.record(moved.getWorkspaceId(), moved.getId(), Actor.user(command.userId()), ActivityType.STORY_MOVED,
                    board.find(fromColumn).map(BoardColumn::getName).orElse(null), board.require(moved.getColumnId()).getName());
        }

        return moved;
    }

    private UserStory place(MoveStoryCommand command, UserStory story, Board board) {
        List<UserStory> column = storyRepository.findAllByColumnId(command.columnId()).stream()
                .filter(other -> !other.getId().equals(story.getId()))
                .toList();
        int index = insertionIndex(column, command.afterStoryId(), command.beforeStoryId());
        String lower = index == 0 ? null : column.get(index - 1).getRank();
        String upper = index == column.size() ? null : column.get(index).getRank();

        if (upper != null && lower != null && lower.compareTo(upper) >= 0) {
            return rebalance(story, column, index, command.columnId());
        }

        String rank = Rank.between(lower, upper);

        if (Rank.isExhausted(rank)) {
            return rebalance(story, column, index, command.columnId());
        }

        story.moveTo(command.columnId(), rank);

        return storyRepository.save(story);
    }

    private static int insertionIndex(List<UserStory> column, UUID afterId, UUID beforeId) {
        if (afterId == null && beforeId == null) {
            return column.size();
        }

        if (afterId != null) {
            int after = indexOf(column, afterId);

            if (beforeId != null && indexOf(column, beforeId) != after + 1) {
                throw new BusinessRuleException("The reference stories must be neighbours in the destination column");
            }

            return after + 1;
        }

        return indexOf(column, beforeId);
    }

    private static int indexOf(List<UserStory> column, UUID storyId) {
        for (int index = 0; index < column.size(); index++) {
            if (column.get(index).getId().equals(storyId)) {
                return index;
            }
        }

        throw new BusinessRuleException("The reference story is not in the destination column");
    }

    private UserStory rebalance(UserStory story, List<UserStory> column, int index, UUID columnId) {
        List<UserStory> ordered = new ArrayList<>(column);
        ordered.add(index, story);
        List<String> ranks = Rank.spread(ordered.size());

        for (int position = 0; position < ordered.size(); position++) {
            ordered.get(position).moveTo(columnId, ranks.get(position));
        }

        storyRepository.saveAll(ordered);

        return story;
    }
}
