package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ListActivityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListActivityQuery;
import io.github.arthursouto.prumo.application.port.in.result.ActivityView;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ListActivityUseCase listActivity;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityController(listActivity))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }

    @Test
    void theStoryHistoryCarriesWhoWhatAndWhetherGithubDidIt() throws Exception {
        UUID actor = UUID.randomUUID();
        Activity byPerson = Activity.of(workspaceId, UUID.randomUUID(), null, actor, ChangeSource.USER, ActivityType.STORY_MOVED, "To Do", "In Progress");
        Activity byGithub = Activity.of(workspaceId, UUID.randomUUID(), "PRU-13", null, ChangeSource.GITHUB, ActivityType.SUBTASK_STATUS_CHANGED, "TO_DO", "IN_REVIEW");
        when(listActivity.execute(ListActivityQuery.ofStory(userId, workspaceId, "PRU-12"))).thenReturn(List.of(
                new ActivityView(byGithub, null, "TO_DO", "IN_REVIEW"),
                new ActivityView(byPerson, "Arthur", "To Do", "In Progress")));

        mockMvc.perform(get(base + "/stories/PRU-12/activity").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].source").value("GITHUB"))
                .andExpect(jsonPath("$[0].actorName").doesNotExist())
                .andExpect(jsonPath("$[0].subtaskKey").value("PRU-13"))
                .andExpect(jsonPath("$[0].type").value("SUBTASK_STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].source").value("USER"))
                .andExpect(jsonPath("$[1].actorName").value("Arthur"))
                .andExpect(jsonPath("$[1].actorId").value(actor.toString()))
                .andExpect(jsonPath("$[1].from").value("To Do"))
                .andExpect(jsonPath("$[1].to").value("In Progress"));
    }

    @Test
    void aSubtaskHistoryIsAskedByTheSubtaskKey() throws Exception {
        when(listActivity.execute(ListActivityQuery.ofSubtask(userId, workspaceId, "PRU-13"))).thenReturn(List.of());

        mockMvc.perform(get(base + "/subtasks/PRU-13/activity").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(listActivity).execute(ListActivityQuery.ofSubtask(userId, workspaceId, "PRU-13"));
    }

    @Test
    void aMissingStoryIsNotFound() throws Exception {
        when(listActivity.execute(ListActivityQuery.ofStory(userId, workspaceId, "PRU-99"))).thenThrow(new ResourceNotFoundException("Story not found"));

        mockMvc.perform(get(base + "/stories/PRU-99/activity").principal(principal()))
                .andExpect(status().isNotFound());
    }
}
