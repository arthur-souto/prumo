package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateStoryBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.GetStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.ListStoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.MoveStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.SetChecklistItemDoneUseCase;
import io.github.arthursouto.prumo.application.port.in.SuggestBranchNameUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ChecklistInput;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryBranchCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.MoveStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.SetChecklistItemDoneCommand;
import io.github.arthursouto.prumo.application.port.in.command.SuggestBranchNameQuery;
import io.github.arthursouto.prumo.application.port.in.command.LinkStoryRepositoryCommand;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoryControllerTest {

    @Mock
    private CreateStoryUseCase createStory;

    @Mock
    private ListStoriesUseCase listStories;

    @Mock
    private GetStoryUseCase getStory;

    @Mock
    private UpdateStoryUseCase updateStory;

    @Mock
    private DeleteStoryUseCase deleteStory;

    @Mock
    private SetChecklistItemDoneUseCase setChecklistItemDone;

    @Mock
    private MoveStoryUseCase moveStory;

    @Mock
    private LinkStoryRepositoryUseCase linkRepository;

    @Mock
    private SuggestBranchNameUseCase suggestBranch;

    @Mock
    private CreateStoryBranchUseCase createBranch;

    @Mock
    private CreateStoryRepositoryUseCase createRepository;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID columnId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId + "/stories";
    private UserStory story;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StoryController(
                        createStory,
                        listStories,
                        getStory,
                        updateStory,
                        deleteStory,
                        setChecklistItemDone,
                        moveStory,
                        linkRepository,
                        suggestBranch,
                        createBranch,
                        createRepository
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        story = UserStory.create(
                workspaceId,
                12,
                "PRU-12",
                "Reset password",
                "# md",
                StoryPriority.HIGH,
                5,
                columnId,
                "V",
                List.of("api"),
                List.of(ChecklistItem.create("code", false)),
                List.of(ChecklistItem.create("works", true))
        );
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(userId.toString(), null);
    }

    @Test
    void createReturnsTheStoryWithItsKeyAndLocation() throws Exception {
        when(createStory.execute(any(CreateStoryCommand.class))).thenReturn(story);

        mockMvc.perform(post(base)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Reset password","priority":"HIGH","storyPoints":5,"labels":["api"],
                                 "subtasks":[{"text":"code"}],"acceptanceCriteria":[{"text":"works","done":true}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + base + "/PRU-12"))
                .andExpect(jsonPath("$.key").value("PRU-12"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.subtasks[0].text").value("code"))
                .andExpect(jsonPath("$.acceptanceCriteria[0].done").value(true))
                .andExpect(jsonPath("$.pullRequests.length()").value(0))
                .andExpect(jsonPath("$.rank").doesNotExist());

        ArgumentCaptor<CreateStoryCommand> command = ArgumentCaptor.forClass(CreateStoryCommand.class);
        verify(createStory).execute(command.capture());
        assertThat(command.getValue().userId()).isEqualTo(userId);
        assertThat(command.getValue().workspaceId()).isEqualTo(workspaceId);
        assertThat(command.getValue().subtasks()).containsExactly(new ChecklistInput(null, "code", false));
        assertThat(command.getValue().acceptanceCriteria()).containsExactly(new ChecklistInput(null, "works", true));
    }

    @Test
    void createValidatesTheBody() throws Exception {
        mockMvc.perform(post(base)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","storyPoints":-1,"labels":["%s"],"subtasks":[{"text":""}]}
                                """.formatted("x".repeat(41))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));

        verifyNoInteractions(createStory);
    }

    @Test
    void listPassesTheFilters() throws Exception {
        when(listStories.execute(new ListStoriesQuery(userId, workspaceId, "api", StoryPriority.HIGH, "reset")))
                .thenReturn(List.of(story));

        mockMvc.perform(get(base).principal(principal()).param("label", "api").param("priority", "HIGH").param("q", "reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("PRU-12"));
    }

    @Test
    void listRejectsAnUnknownPriority() throws Exception {
        mockMvc.perform(get(base).principal(principal()).param("priority", "HUGE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listStories);
    }

    @Test
    void getMapsMissingStoriesTo404() throws Exception {
        when(getStory.execute(any())).thenThrow(new ResourceNotFoundException("Story not found"));

        mockMvc.perform(get(base + "/PRU-99").principal(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Story not found"));
    }

    @Test
    void updateReplacesTheFieldsAndKeepsItemIds() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(updateStory.execute(any())).thenReturn(story);

        mockMvc.perform(put(base + "/PRU-12")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New","priority":"LOW","subtasks":[{"id":"%s","text":"a","done":true}]}
                                """.formatted(itemId)))
                .andExpect(status().isOk());

        ArgumentCaptor<io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand> command =
                ArgumentCaptor.forClass(io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand.class);
        verify(updateStory).execute(command.capture());
        assertThat(command.getValue().key()).isEqualTo("PRU-12");
        assertThat(command.getValue().subtasks()).containsExactly(new ChecklistInput(itemId, "a", true));
        assertThat(command.getValue().acceptanceCriteria()).isEmpty();
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(base + "/PRU-12").principal(principal())).andExpect(status().isNoContent());

        verify(deleteStory).execute(new DeleteStoryCommand(userId, workspaceId, "PRU-12"));
    }

    @Test
    void tickingAChecklistItemRequiresTheDoneFlag() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(setChecklistItemDone.execute(new SetChecklistItemDoneCommand(userId, workspaceId, "PRU-12", itemId, true)))
                .thenReturn(story);

        mockMvc.perform(patch(base + "/PRU-12/checklist/" + itemId)
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(base + "/PRU-12/checklist/" + itemId)
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveSendsTheColumnAndTheNeighbours() throws Exception {
        UUID after = UUID.randomUUID();
        UUID before = UUID.randomUUID();
        when(moveStory.execute(new MoveStoryCommand(userId, workspaceId, "PRU-12", columnId, after, before)))
                .thenReturn(story);

        mockMvc.perform(put(base + "/PRU-12/move")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"columnId":"%s","afterStoryId":"%s","beforeStoryId":"%s"}
                                """.formatted(columnId, after, before)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(columnId.toString()));
    }

    @Test
    void moveRequiresAColumnAndMapsRuleViolationsTo422() throws Exception {
        mockMvc.perform(put(base + "/PRU-12/move")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        when(moveStory.execute(any())).thenThrow(new BusinessRuleException("The reference story is not in the destination column"));

        mockMvc.perform(put(base + "/PRU-12/move")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":\"" + columnId + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void linkAndUnlinkARepository() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(linkRepository.execute(any())).thenReturn(story);

        mockMvc.perform(put(base + "/PRU-12/repository")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryId\":\"" + repositoryId + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(base + "/PRU-12/repository")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(linkRepository).execute(new LinkStoryRepositoryCommand(userId, workspaceId, "PRU-12", repositoryId));
        verify(linkRepository).execute(new LinkStoryRepositoryCommand(userId, workspaceId, "PRU-12", null));
    }

    @Test
    void suggestsABranchName() throws Exception {
        when(suggestBranch.execute(new SuggestBranchNameQuery(userId, workspaceId, "PRU-12")))
                .thenReturn("feature/PRU-12-reset-password");

        mockMvc.perform(get(base + "/PRU-12/branch-suggestion").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("feature/PRU-12-reset-password"));
    }

    @Test
    void branchNamesAreValidated() throws Exception {
        for (String bad : new String[] {"", "-x", "a b", "x;rm -rf", "a..b/", "name/"}) {
            mockMvc.perform(post(base + "/PRU-12/branch")
                            .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + bad + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(createBranch);
    }

    @Test
    void createsABranchAndReturnsTheStoryWithTheBranch() throws Exception {
        when(createBranch.execute(new CreateStoryBranchCommand(userId, workspaceId, "PRU-12", "feature/PRU-12-x")))
                .thenReturn(story);

        mockMvc.perform(post(base + "/PRU-12/branch")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"feature/PRU-12-x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createsARepositoryForTheStory() throws Exception {
        when(createRepository.execute(new CreateStoryRepositoryCommand(userId, workspaceId, "PRU-12", "pru-12", null, true)))
                .thenReturn(story);

        mockMvc.perform(post(base + "/PRU-12/new-repository")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pru-12\",\"isPrivate\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(base + "/PRU-12/new-repository")
                        .principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bad name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pullRequestsAreExposedWithoutInternalIds() throws Exception {
        UserStory withPr = UserStory.restore(
                story.getId(), workspaceId, 12, "PRU-12", "T", null, StoryPriority.LOW, null, columnId, "V",
                List.of(), List.of(), List.of(), UUID.randomUUID(), "feature/x",
                List.of(PullRequest.track(story.getId(), UUID.randomUUID(), 7, "PR", PullRequestState.MERGED,
                        "https://github.com/a/b/pull/7", "feature/x", Instant.now())),
                Instant.now(), Instant.now()
        );
        when(getStory.execute(any())).thenReturn(withPr);

        mockMvc.perform(get(base + "/PRU-12").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pullRequests[0].number").value(7))
                .andExpect(jsonPath("$.pullRequests[0].state").value("MERGED"))
                .andExpect(jsonPath("$.pullRequests[0].id").doesNotExist())
                .andExpect(jsonPath("$.branchName").value("feature/x"));
    }
}
