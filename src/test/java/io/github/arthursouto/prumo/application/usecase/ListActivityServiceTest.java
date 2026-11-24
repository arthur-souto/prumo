package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.ListActivityQuery;
import io.github.arthursouto.prumo.application.port.in.result.ActivityView;
import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActivityServiceTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private SubtaskRepositoryPort subtaskRepository;

    @Mock
    private ActivityRepositoryPort activityRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private final UUID userId = UUID.randomUUID();
    private Workspace workspace;
    private UserStory story;
    private User ada;
    private ListActivityService service;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        Board board = BoardFixtures.boardFor(workspace);
        story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        ada = User.registerExternal("Ada", "ada@example.com", null);
        WorkspaceAccess access = new WorkspaceAccess(workspaceRepository);
        service = new ListActivityService(
                new StoryLookup(access, storyRepository),
                new SubtaskLookup(access, subtaskRepository, storyRepository),
                activityRepository,
                userRepository
        );
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        lenient().when(userRepository.findById(ada.getId())).thenReturn(Optional.of(ada));
    }

    private Activity entry(UUID actor, ChangeSource source, ActivityType type, String from, String to) {
        return Activity.of(workspace.getId(), story.getId(), null, actor, source, type, from, to);
    }

    @Test
    void theStoryHistoryNamesThePersonBehindEachEntryAndLeavesGithubWithoutAName() {
        when(activityRepository.findByStoryId(story.getId(), 100)).thenReturn(List.of(
                entry(null, ChangeSource.GITHUB, ActivityType.STORY_MOVED, "In Progress", "In Review"),
                entry(ada.getId(), ChangeSource.USER, ActivityType.STORY_MOVED, "To Do", "In Progress")));

        List<ActivityView> history = service.execute(ListActivityQuery.ofStory(userId, workspace.getId(), "pru-1"));

        assertThat(history).extracting(ActivityView::actorName).containsExactly(null, "Ada");
        assertThat(history.get(1).fromLabel()).isEqualTo("To Do");
        assertThat(history.get(1).toLabel()).isEqualTo("In Progress");
    }

    @Test
    void anAssignmentShowsTheNamesOfTheAssigneesInsteadOfTheirIds() {
        when(activityRepository.findByStoryId(story.getId(), 100)).thenReturn(List.of(
                entry(userId, ChangeSource.USER, ActivityType.STORY_ASSIGNED, null, ada.getId().toString())));

        ActivityView view = service.execute(ListActivityQuery.ofStory(userId, workspace.getId(), "PRU-1")).get(0);

        assertThat(view.fromLabel()).isNull();
        assertThat(view.toLabel()).isEqualTo("Ada");
    }

    @Test
    void aSubtaskHistoryIsReadByTheKeyOfTheSubtask() {
        Subtask subtask = Subtask.create(story.getId(), workspace.getId(), 2, "PRU-2", "Task", null, 0);
        when(subtaskRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-2")).thenReturn(Optional.of(subtask));
        when(activityRepository.findBySubtaskKey(workspace.getId(), "PRU-2", 100)).thenReturn(List.of());

        assertThat(service.execute(ListActivityQuery.ofSubtask(userId, workspace.getId(), "pru-2"))).isEmpty();

        verify(activityRepository, never()).findByStoryId(story.getId(), 100);
    }

    @Test
    void anUnknownStoryIsNotFound() {
        assertThatThrownBy(() -> service.execute(ListActivityQuery.ofStory(userId, workspace.getId(), "PRU-9")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
