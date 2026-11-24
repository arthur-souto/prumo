package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListCommentsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListCommentsQuery;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;

public class ListCommentsService implements ListCommentsUseCase {

    private final StoryLookup stories;
    private final SubtaskLookup subtasks;
    private final CommentRepositoryPort commentRepository;
    private final CommentAuthors authors;

    public ListCommentsService(StoryLookup stories, SubtaskLookup subtasks, CommentRepositoryPort commentRepository, CommentAuthors authors) {
        this.stories = stories;
        this.subtasks = subtasks;
        this.commentRepository = commentRepository;
        this.authors = authors;
    }

    @Override
    public List<CommentView> execute(ListCommentsQuery query) {
        if (query.subtaskKey() != null) {
            Subtask subtask = subtasks.asMember(query.userId(), query.workspaceId(), query.subtaskKey());

            return authors.views(commentRepository.findOfSubtask(query.workspaceId(), subtask.getKey()));
        }

        UserStory story = stories.asMember(query.userId(), query.workspaceId(), query.storyKey());

        return authors.views(commentRepository.findOfStory(story.getId()));
    }
}
