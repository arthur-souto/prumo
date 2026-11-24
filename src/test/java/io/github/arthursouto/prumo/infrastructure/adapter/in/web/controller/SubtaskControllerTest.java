package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ChangeSubtaskStatusUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.GetSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.ListSubtasksUseCase;
import io.github.arthursouto.prumo.application.port.in.SuggestSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ChangeSubtaskStatusCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskBranchCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.SuggestSubtaskBranchQuery;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubtaskControllerTest {

    @Mock
    private CreateSubtaskUseCase createSubtask;

    @Mock
    private ListSubtasksUseCase listSubtasks;

    @Mock
    private GetSubtaskUseCase getSubtask;

    @Mock
    private UpdateSubtaskUseCase updateSubtask;

    @Mock
    private ChangeSubtaskStatusUseCase changeStatus;

    @Mock
    private DeleteSubtaskUseCase deleteSubtask;

    @Mock
    private SuggestSubtaskBranchUseCase suggestBranch;

    @Mock
    private CreateSubtaskBranchUseCase createBranch;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId;
    private SubtaskView view;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SubtaskController(
                        createSubtask, listSubtasks, getSubtask, updateSubtask, changeStatus, deleteSubtask, suggestBranch, createBranch
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        Subtask subtask = Subtask.create(UUID.randomUUID(), workspaceId, 13, "PRU-13", "Write the endpoint", "# md", 0);
        subtask.recordBranch("feature/PRU-13-write-the-endpoint");
        PullRequest pullRequest = PullRequest.track(subtask.getStoryId(), UUID.randomUUID(), 7, "Add endpoint",
                PullRequestState.OPEN, "https://github.com/a/b/pull/7", "feature/PRU-13-write-the-endpoint", Instant.now())
                .forSubtask(subtask.getId());
        view = new SubtaskView(subtask, "PRU-12", List.of(pullRequest));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }

    @Test
    void createReturnsTheSubtaskWithItsKeyLocationBranchAndPullRequests() throws Exception {
        when(createSubtask.execute(any(CreateSubtaskCommand.class))).thenReturn(view);

        mockMvc.perform(post(base + "/stories/PRU-12/subtasks")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Write the endpoint","description":"# md","createBranch":true,
                                 "branchName":"feature/PRU-13-write-the-endpoint"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + base + "/subtasks/PRU-13"))
                .andExpect(jsonPath("$.key").value("PRU-13"))
                .andExpect(jsonPath("$.storyKey").value("PRU-12"))
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.branchName").value("feature/PRU-13-write-the-endpoint"))
                .andExpect(jsonPath("$.pullRequests[0].number").value(7))
                .andExpect(jsonPath("$.pullRequests[0].state").value("OPEN"));

        ArgumentCaptor<CreateSubtaskCommand> command = ArgumentCaptor.forClass(CreateSubtaskCommand.class);
        verify(createSubtask).execute(command.capture());
        assertThat(command.getValue().storyKey()).isEqualTo("PRU-12");
        assertThat(command.getValue().createBranch()).isTrue();
        assertThat(command.getValue().branchName()).isEqualTo("feature/PRU-13-write-the-endpoint");
    }

    @Test
    void createWithoutABranchDefaultsToNone() throws Exception {
        when(createSubtask.execute(any(CreateSubtaskCommand.class))).thenReturn(view);

        mockMvc.perform(post(base + "/stories/PRU-12/subtasks")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateSubtaskCommand> command = ArgumentCaptor.forClass(CreateSubtaskCommand.class);
        verify(createSubtask).execute(command.capture());
        assertThat(command.getValue().createBranch()).isFalse();
    }

    @Test
    void createValidatesTheBody() throws Exception {
        mockMvc.perform(post(base + "/stories/PRU-12/subtasks")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"createBranch\":true,\"branchName\":\"bad name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(createSubtask);
    }

    @Test
    void aMissingRepositoryIsABusinessRuleError() throws Exception {
        when(createSubtask.execute(any(CreateSubtaskCommand.class)))
                .thenThrow(new BusinessRuleException("Link a repository to the story before creating a branch"));

        mockMvc.perform(post(base + "/stories/PRU-12/subtasks")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task\",\"createBranch\":true,\"branchName\":\"feature/x\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void githubFailuresKeepTheirCode() throws Exception {
        when(createSubtask.execute(any(CreateSubtaskCommand.class)))
                .thenThrow(new GithubIntegrationException(GithubFailure.BRANCH_EXISTS, "exists"));

        mockMvc.perform(post(base + "/stories/PRU-12/subtasks")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task\",\"createBranch\":true,\"branchName\":\"feature/x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRANCH_EXISTS"));
    }

    @Test
    void listsTheSubtasksOfAStory() throws Exception {
        when(listSubtasks.execute(any())).thenReturn(List.of(view));

        mockMvc.perform(get(base + "/stories/PRU-12/subtasks").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").value("PRU-13"));
    }

    @Test
    void getsASubtaskByKeyOrReportsNotFound() throws Exception {
        when(getSubtask.execute(any())).thenReturn(view).thenThrow(new ResourceNotFoundException("Subtask not found"));

        mockMvc.perform(get(base + "/subtasks/PRU-13").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write the endpoint"));
        mockMvc.perform(get(base + "/subtasks/PRU-99").principal(principal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReplacesTitleAndDescription() throws Exception {
        when(updateSubtask.execute(any())).thenReturn(view);

        mockMvc.perform(put(base + "/subtasks/PRU-13")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"description\":\"d\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(base + "/subtasks/PRU-13")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesTheStatusAndRejectsUnknownOnes() throws Exception {
        when(changeStatus.execute(any())).thenReturn(view);

        mockMvc.perform(put(base + "/subtasks/PRU-13/status")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(base + "/subtasks/PRU-13/status")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(base + "/subtasks/PRU-13/status")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        ArgumentCaptor<ChangeSubtaskStatusCommand> command = ArgumentCaptor.forClass(ChangeSubtaskStatusCommand.class);
        verify(changeStatus).execute(command.capture());
        assertThat(command.getValue().status()).isEqualTo(SubtaskStatus.IN_REVIEW);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(base + "/subtasks/PRU-13").principal(principal()))
                .andExpect(status().isNoContent());

        verify(deleteSubtask).execute(new DeleteSubtaskCommand(userId, workspaceId, "PRU-13"));
    }

    @Test
    void suggestsAndCreatesTheBranchOfAnExistingSubtask() throws Exception {
        when(suggestBranch.execute(new SuggestSubtaskBranchQuery(userId, workspaceId, "PRU-13")))
                .thenReturn("feature/PRU-13-write-the-endpoint");
        when(createBranch.execute(new CreateSubtaskBranchCommand(userId, workspaceId, "PRU-13", "feature/PRU-13-x")))
                .thenReturn(view);

        mockMvc.perform(get(base + "/subtasks/PRU-13/branch-suggestion").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("feature/PRU-13-write-the-endpoint"));
        mockMvc.perform(post(base + "/subtasks/PRU-13/branch")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"feature/PRU-13-x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchName").value("feature/PRU-13-write-the-endpoint"));
        mockMvc.perform(post(base + "/subtasks/PRU-13/branch")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bad name\"}"))
                .andExpect(status().isBadRequest());
    }
}
