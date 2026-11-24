package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.AddCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AddCommentCommand;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class AddCommentService implements AddCommentUseCase {

    private final StoryLookup stories;
    private final SubtaskLookup subtasks;
    private final CommentRepositoryPort commentRepository;
    private final CommentAuthors authors;

    public AddCommentService(StoryLookup stories, SubtaskLookup subtasks, CommentRepositoryPort commentRepository, CommentAuthors authors) {
        this.stories = stories;
        this.subtasks = subtasks;
        this.commentRepository = commentRepository;
        this.authors = authors;
    }

    @Override
    public CommentView execute(AddCommentCommand command) {
        String body = command.body().strip();
        Comment comment;

        if (command.subtaskKey() != null) {
            Subtask subtask = subtasks.asMember(command.userId(), command.workspaceId(), command.subtaskKey());
            comment = Comment.create(command.workspaceId(), subtask.getStoryId(), subtask.getKey(), command.userId(), body);
        } else {
            UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.storyKey());
            comment = Comment.create(command.workspaceId(), story.getId(), null, command.userId(), body);
        }

        return authors.view(commentRepository.save(comment));
    }
}
