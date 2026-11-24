package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.AddCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.EditCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.ListCommentsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AddCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.EditCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListCommentsQuery;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private ListCommentsUseCase listComments;

    @Mock
    private AddCommentUseCase addComment;

    @Mock
    private EditCommentUseCase editComment;

    @Mock
    private DeleteCommentUseCase deleteComment;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(listComments, addComment, editComment, deleteComment))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }

    private CommentView view(UUID author, String subtaskKey, String body) {
        return new CommentView(Comment.create(workspaceId, UUID.randomUUID(), subtaskKey, author, body), "Ada", "https://img/ada.png");
    }

    @Test
    void commentingOnAStoryReturns201WithTheAuthorAndMineSet() throws Exception {
        when(addComment.execute(new AddCommentCommand(userId, workspaceId, "PRU-1", null, "**hello**"))).thenReturn(view(userId, null, "**hello**"));

        mockMvc.perform(post(base + "/stories/PRU-1/comments").principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"**hello**\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("**hello**"))
                .andExpect(jsonPath("$.authorName").value("Ada"))
                .andExpect(jsonPath("$.authorId").value(userId.toString()))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.editedAt").doesNotExist())
                .andExpect(jsonPath("$.subtaskKey").doesNotExist());
    }

    @Test
    void commentingOnASubtaskSendsItsKeyAndNotTheStoryKey() throws Exception {
        when(addComment.execute(new AddCommentCommand(userId, workspaceId, null, "PRU-2", "ok"))).thenReturn(view(userId, "PRU-2", "ok"));

        mockMvc.perform(post(base + "/subtasks/PRU-2/comments").principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"ok\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtaskKey").value("PRU-2"));
    }

    @Test
    void aBlankOrTooLongBodyIsRejectedBeforeReachingTheUseCase() throws Exception {
        for (String body : new String[] {"", "   ", "x".repeat(20001)}) {
            mockMvc.perform(post(base + "/stories/PRU-1/comments").principal(principal())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(put(base + "/comments/" + UUID.randomUUID()).principal(principal())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(addComment, editComment);
    }

    @Test
    void listingMarksOnlyTheCallersOwnCommentsAsMine() throws Exception {
        when(listComments.execute(new ListCommentsQuery(userId, workspaceId, "PRU-1", null)))
                .thenReturn(List.of(view(userId, null, "mine"), view(UUID.randomUUID(), null, "theirs")));

        mockMvc.perform(get(base + "/stories/PRU-1/comments").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[1].mine").value(false));
    }

    @Test
    void listingASubtaskAsksByTheSubtaskKey() throws Exception {
        when(listComments.execute(new ListCommentsQuery(userId, workspaceId, null, "PRU-2"))).thenReturn(List.of());

        mockMvc.perform(get(base + "/subtasks/PRU-2/comments").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void editingReturnsTheUpdatedCommentAndSomeoneElsesCommentIs403() throws Exception {
        UUID commentId = UUID.randomUUID();
        when(editComment.execute(new EditCommentCommand(userId, workspaceId, commentId, "new"))).thenReturn(view(userId, null, "new"));

        mockMvc.perform(put(base + "/comments/" + commentId).principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("new"));

        when(editComment.execute(any(EditCommentCommand.class))).thenThrow(new ForbiddenException("Only the author can change this comment"));

        mockMvc.perform(put(base + "/comments/" + UUID.randomUUID()).principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"new\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingReturns204AndMapsTheErrorsTo403And404() throws Exception {
        UUID commentId = UUID.randomUUID();

        mockMvc.perform(delete(base + "/comments/" + commentId).principal(principal()))
                .andExpect(status().isNoContent());
        verify(deleteComment).execute(new DeleteCommentCommand(userId, workspaceId, commentId));

        org.mockito.Mockito.doThrow(new ForbiddenException("no")).when(deleteComment).execute(new DeleteCommentCommand(userId, workspaceId, UUID.fromString("00000000-0000-0000-0000-000000000001")));
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("gone")).when(deleteComment).execute(new DeleteCommentCommand(userId, workspaceId, UUID.fromString("00000000-0000-0000-0000-000000000002")));

        mockMvc.perform(delete(base + "/comments/00000000-0000-0000-0000-000000000001").principal(principal())).andExpect(status().isForbidden());
        mockMvc.perform(delete(base + "/comments/00000000-0000-0000-0000-000000000002").principal(principal())).andExpect(status().isNotFound());
    }
}
