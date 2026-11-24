package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;
import java.util.UUID;

/** Status changes of a subtask and what they cause on the story, shared by the API and the pull request sync. */
public class SubtaskWorkflow {

    private final SubtaskRepositoryPort subtaskRepository;
    private final StoryRepositoryPort storyRepository;

    public SubtaskWorkflow(SubtaskRepositoryPort subtaskRepository, StoryRepositoryPort storyRepository) {
        this.subtaskRepository = subtaskRepository;
        this.storyRepository = storyRepository;
    }

    /**
     * Changes the status and, when the board consolidates stories, moves the story to Done in the event that
     * leaves every subtask DONE. Nothing re-opens the story later: a manual move by the user stands.
     */
    public Subtask changeStatus(Board board, Subtask subtask, SubtaskStatus status) {
        return changeStatus(board, subtask, status, ChangeSource.USER);
    }

    /** The source is recorded on the subtask and also on the story when this change is what finishes it. */
    public Subtask changeStatus(Board board, Subtask subtask, SubtaskStatus status, ChangeSource source) {
        if (!subtask.changeStatus(status, source)) {
            return subtask;
        }

        Subtask saved = subtaskRepository.save(subtask);

        if (status == SubtaskStatus.DONE && board.isConsolidateStories()) {
            consolidate(board, saved.getStoryId(), source);
        }

        return saved;
    }

    public void moveStory(Board board, UserStory story, ColumnKind target) {
        moveStory(board, story, target, ChangeSource.USER);
    }

    public void moveStory(Board board, UserStory story, ColumnKind target, ChangeSource source) {
        board.ofKind(target)
                .filter(column -> !column.getId().equals(story.getColumnId()))
                .ifPresent(column -> {
                    List<UserStory> inColumn = storyRepository.findAllByColumnId(column.getId());
                    String last = inColumn.isEmpty() ? null : inColumn.get(inColumn.size() - 1).getRank();

                    story.moveTo(column.getId(), Rank.between(last, null), source);
                    storyRepository.save(story);
                });
    }

    private void consolidate(Board board, UUID storyId, ChangeSource source) {
        if (!Subtask.allDone(subtaskRepository.findAllByStoryId(storyId))) {
            return;
        }

        storyRepository.findById(storyId).ifPresent(story -> moveStory(board, story, ColumnKind.DONE, source));
    }
}
