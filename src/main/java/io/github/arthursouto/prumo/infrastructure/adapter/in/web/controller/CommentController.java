package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.AddCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.EditCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.ListCommentsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AddCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.EditCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListCommentsQuery;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.CommentApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.AddCommentRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CommentResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.EditCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CommentController implements CommentApi {

    private final ListCommentsUseCase listComments;
    private final AddCommentUseCase addComment;
    private final EditCommentUseCase editComment;
    private final DeleteCommentUseCase deleteComment;

    public CommentController(ListCommentsUseCase listComments,
                             AddCommentUseCase addComment,
                             EditCommentUseCase editComment,
                             DeleteCommentUseCase deleteComment) {
        this.listComments = listComments;
        this.addComment = addComment;
        this.editComment = editComment;
        this.deleteComment = deleteComment;
    }

    @Override
    public List<CommentResponse> ofStory(Authentication authentication, UUID workspaceId, String key) {
        UUID userId = userId(authentication);

        return listComments.execute(new ListCommentsQuery(userId, workspaceId, key, null)).stream()
                .map(view -> CommentResponse.from(view, userId))
                .toList();
    }

    @Override
    public ResponseEntity<CommentResponse> addToStory(Authentication authentication, UUID workspaceId, String key, AddCommentRequest request) {
        UUID userId = userId(authentication);
        CommentResponse created = CommentResponse.from(addComment.execute(new AddCommentCommand(userId, workspaceId, key, null, request.body())), userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public List<CommentResponse> ofSubtask(Authentication authentication, UUID workspaceId, String key) {
        UUID userId = userId(authentication);

        return listComments.execute(new ListCommentsQuery(userId, workspaceId, null, key)).stream()
                .map(view -> CommentResponse.from(view, userId))
                .toList();
    }

    @Override
    public ResponseEntity<CommentResponse> addToSubtask(Authentication authentication, UUID workspaceId, String key, AddCommentRequest request) {
        UUID userId = userId(authentication);
        CommentResponse created = CommentResponse.from(addComment.execute(new AddCommentCommand(userId, workspaceId, null, key, request.body())), userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public CommentResponse edit(Authentication authentication, UUID workspaceId, UUID commentId, EditCommentRequest request) {
        UUID userId = userId(authentication);

        return CommentResponse.from(editComment.execute(new EditCommentCommand(userId, workspaceId, commentId, request.body())), userId);
    }

    @Override
    public ResponseEntity<Void> delete(Authentication authentication, UUID workspaceId, UUID commentId) {
        deleteComment.execute(new DeleteCommentCommand(userId(authentication), workspaceId, commentId));

        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
