package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CloseSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.GetSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.ListSprintsUseCase;
import io.github.arthursouto.prumo.application.port.in.PlanStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.StartSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListSprintsQuery;
import io.github.arthursouto.prumo.application.port.in.command.PlanStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
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
class SprintControllerTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 14);

    @Mock
    private ListSprintsUseCase listSprints;

    @Mock
    private GetSprintUseCase getSprint;

    @Mock
    private CreateSprintUseCase createSprint;

    @Mock
    private UpdateSprintUseCase updateSprint;

    @Mock
    private StartSprintUseCase startSprint;

    @Mock
    private CloseSprintUseCase closeSprint;

    @Mock
    private DeleteSprintUseCase deleteSprint;

    @Mock
    private PlanStoryUseCase planStory;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SprintController(
                        listSprints, getSprint, createSprint, updateSprint, startSprint, closeSprint, deleteSprint, planStory))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }

    private Sprint sprint() {
        return Sprint.create(workspaceId, "Sprint 7", "Ship", START, END);
    }

    @Test
    void searchesAPageOfSprintsWithTheirPoints() throws Exception {
        Sprint sprint = sprint();
        sprint.start(13);
        when(listSprints.execute(new ListSprintsQuery(userId, workspaceId, "sprint", List.of(SprintState.ACTIVE, SprintState.CLOSED), 1, 10)))
                .thenReturn(new PageResult<>(List.of(new SprintView(sprint, 4, 13, 5)), 1, 10, 11));

        mockMvc.perform(get(base + "/sprints").principal(principal())
                        .param("q", "sprint").param("state", "ACTIVE", "CLOSED").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Sprint 7"))
                .andExpect(jsonPath("$.items[0].state").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.items[0].committedPoints").value(13))
                .andExpect(jsonPath("$.items[0].completedPoints").doesNotExist())
                .andExpect(jsonPath("$.items[0].storyCount").value(4))
                .andExpect(jsonPath("$.items[0].donePoints").value(5));
    }

    @Test
    void rejectsAnOversizedPage() throws Exception {
        mockMvc.perform(get(base + "/sprints").principal(principal()).param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listSprints);
    }

    @Test
    void getsASprint() throws Exception {
        Sprint sprint = sprint();
        when(getSprint.execute(new GetSprintQuery(userId, workspaceId, sprint.getId()))).thenReturn(new SprintView(sprint, 2, 8, 3));

        mockMvc.perform(get(base + "/sprints/" + sprint.getId()).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sprint.getId().toString()))
                .andExpect(jsonPath("$.totalPoints").value(8));
    }

    @Test
    void createsASprint() throws Exception {
        Sprint sprint = sprint();
        when(createSprint.execute(new CreateSprintCommand(userId, workspaceId, "Sprint 7", "Ship", START, END)))
                .thenReturn(new SprintView(sprint, 0, 0, 0));

        mockMvc.perform(post(base + "/sprints").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sprint 7\",\"goal\":\"Ship\",\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-14\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/sprints/" + sprint.getId())))
                .andExpect(jsonPath("$.state").value("PLANNED"));
    }

    @Test
    void rejectsAnInvalidSprint() throws Exception {
        for (String body : List.of(
                "{\"name\":\" \",\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-14\"}",
                "{\"name\":\"S\",\"endDate\":\"2026-12-14\"}",
                "{\"name\":\"S\",\"startDate\":\"2026-12-14\",\"endDate\":\"2026-12-01\"}"
        )) {
            mockMvc.perform(post(base + "/sprints").principal(principal()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(createSprint);
    }

    @Test
    void startingWhileAnotherIsActiveIsAConflict() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(startSprint.execute(new SprintCommand(userId, workspaceId, sprintId))).thenThrow(new ConflictException("Another sprint is already active"));

        mockMvc.perform(post(base + "/sprints/" + sprintId + "/start").principal(principal()))
                .andExpect(status().isConflict());
    }

    @Test
    void closingASprintThatIsNotActiveBreaksARule() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(closeSprint.execute(new SprintCommand(userId, workspaceId, sprintId))).thenThrow(new BusinessRuleException("Only the active sprint can be closed"));

        mockMvc.perform(post(base + "/sprints/" + sprintId + "/close").principal(principal()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deletesASprint() throws Exception {
        UUID sprintId = UUID.randomUUID();

        mockMvc.perform(delete(base + "/sprints/" + sprintId).principal(principal()))
                .andExpect(status().isNoContent());

        verify(deleteSprint).execute(new SprintCommand(userId, workspaceId, sprintId));
    }

    @Test
    void plansAStoryIntoASprintOrBackToTheBacklog() throws Exception {
        UUID sprintId = UUID.randomUUID();
        UserStory story = UserStory.create(workspaceId, 1, "PRU-1", "Story", null, StoryPriority.MEDIUM, null,
                UUID.randomUUID(), "a", List.of(), List.of()).withSprint(sprintId);
        when(planStory.execute(any(PlanStoryCommand.class))).thenReturn(story);

        mockMvc.perform(put(base + "/stories/PRU-1/sprint").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":\"" + sprintId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(sprintId.toString()));
        mockMvc.perform(put(base + "/stories/PRU-1/sprint").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":null}"))
                .andExpect(status().isOk());

        verify(planStory).execute(new PlanStoryCommand(userId, workspaceId, "PRU-1", sprintId));
        verify(planStory).execute(new PlanStoryCommand(userId, workspaceId, "PRU-1", null));
    }
}
