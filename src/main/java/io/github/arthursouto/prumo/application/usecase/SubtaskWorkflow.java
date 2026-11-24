package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;
import java.util.UUID;

/** Status changes of a subtask and what they cause on the story, shared by the API and the pull request sync. */
public class SubtaskWorkflow {

    private final SubtaskRepositoryPort subtaskRepository;
    private final StoryRepositoryPort storyRepository;
    private final ActivityLog activity;

    public SubtaskWorkflow(SubtaskRepositoryPort subtaskRepository, StoryRepositoryPort storyRepository, ActivityLog activity) {
        this.subtaskRepository = subtaskRepository;
        this.storyRepository = storyRepository;
        this.activity = activity;
    }

    /**
     * Changes the status and, when the board consolidates stories, moves the story to Done in the event that
     * leaves every subtask DONE. Nothing re-opens the story later: a manual move by the user stands. The actor is
     * recorded on the subtask, on the story it finishes and in the history.
     */
    public Subtask changeStatus(Board board, Subtask subtask, SubtaskStatus status, Actor actor) {
        SubtaskStatus before = subtask.getStatus();

        if (!subtask.changeStatus(status, actor.source())) {
            return subtask;
        }

        Subtask saved = subtaskRepository.save(subtask);
        activity.record(saved.getWorkspaceId(), saved.getStoryId(), saved.getKey(), actor,
                ActivityType.SUBTASK_STATUS_CHANGED, before.name(), status.name());

        if (status == SubtaskStatus.DONE && board.isConsolidateStories()) {
            consolidate(board, saved.getStoryId(), actor);
        }

        return saved;
    }

    public void moveStory(Board board, UserStory story, ColumnKind target, Actor actor) {
        board.ofKind(target)
                .filter(column -> !column.getId().equals(story.getColumnId()))
                .ifPresent(column -> {
                    String from = board.find(story.getColumnId()).map(BoardColumn::getName).orElse(null);
                    List<UserStory> inColumn = storyRepository.findAllByColumnId(column.getId());
                    String last = inColumn.isEmpty() ? null : inColumn.get(inColumn.size() - 1).getRank();

                    story.moveTo(column.getId(), Rank.between(last, null), actor.source());
                    storyRepository.save(story);
                    activity.record(story.getWorkspaceId(), story.getId(), actor, ActivityType.STORY_MOVED, from, column.getName());
                });
    }

    private void consolidate(Board board, UUID storyId, Actor actor) {
        if (!Subtask.allDone(subtaskRepository.findAllByStoryId(storyId))) {
            return;
        }

        storyRepository.findById(storyId).ifPresent(story -> moveStory(board, story, ColumnKind.DONE, actor));
    }
}
