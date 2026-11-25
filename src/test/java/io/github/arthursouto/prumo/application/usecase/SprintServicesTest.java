package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.CreateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListSprintsQuery;
import io.github.arthursouto.prumo.application.port.in.command.PlanStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintServicesTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 14);

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private SprintRepositoryPort sprintRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private SprintSnapshotRepositoryPort snapshotRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-12-03T02:30:00Z"), ZoneOffset.UTC);

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private WorkspaceAccess access;
    private SprintLookup lookup;
    private SprintViews views;
    private SprintSnapshots snapshots;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        board = BoardFixtures.boardFor(workspace);
        access = new WorkspaceAccess(workspaceRepository);
        lookup = new SprintLookup(access, sprintRepository);
        views = new SprintViews(boardRepository, storyRepository);
        snapshots = new SprintSnapshots(storyRepository, snapshotRepository, views, clock, ZoneId.of("America/Sao_Paulo"));
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(sprintRepository.save(any(Sprint.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of());
    }

    private Sprint sprint(SprintState state) {
        Sprint sprint = Sprint.create(workspace.getId(), "Sprint 1", null, START, END);
        if (state != SprintState.PLANNED) {
            sprint.start(0);
        }
        if (state == SprintState.CLOSED) {
            sprint.close(0);
        }
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        return sprint;
    }

    private UserStory story(int number, Integer points, ColumnKind kind, Sprint sprint) {
        UserStory story = UserStory.create(workspace.getId(), number, "PRU-" + number, "Story " + number, null,
                StoryPriority.MEDIUM, points, BoardFixtures.column(board, kind).getId(), "r" + number, List.of(), List.of());
        return sprint == null ? story : story.withSprint(sprint.getId());
    }

    @Test
    void createsAPlannedSprintWithTheTextTrimmed() {
        SprintView view = new CreateSprintService(access, sprintRepository, views)
                .execute(new CreateSprintCommand(ownerId, workspace.getId(), "  Sprint 7 ", "   ", START, END));

        assertThat(view.sprint().getName()).isEqualTo("Sprint 7");
        assertThat(view.sprint().getGoal()).isNull();
        assertThat(view.sprint().getState()).isEqualTo(SprintState.PLANNED);
        assertThat(view.storyCount()).isZero();
    }

    @Test
    void onlyEditorsManageSprints() {
        CreateSprintService create = new CreateSprintService(access, sprintRepository, views);

        assertThatThrownBy(() -> create.execute(new CreateSprintCommand(memberId, workspace.getId(), "S", null, START, END)))
                .isInstanceOf(ForbiddenException.class);
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void aSprintOfAnotherWorkspaceIsNotFound() {
        Sprint other = Sprint.create(UUID.randomUUID(), "Other", null, START, END);
        when(sprintRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> new StartSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), other.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchesAPageOfSprintsWithTheirPoints() {
        Sprint active = Sprint.create(workspace.getId(), "Now", null, START, END);
        active.start(8);
        Sprint next = Sprint.create(workspace.getId(), "Next", null, START.plusDays(14), END.plusDays(14));
        when(sprintRepository.search(new SprintSearchCriteria(workspace.getId(), "now", EnumSet.of(SprintState.ACTIVE, SprintState.PLANNED), 1, 2)))
                .thenReturn(new PageResult<>(List.of(active, next), 1, 2, 5));
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(
                story(1, 5, ColumnKind.DONE, active),
                story(2, 3, ColumnKind.IN_PROGRESS, active),
                story(3, null, ColumnKind.TODO, active)
        ));

        PageResult<SprintView> page = new ListSprintsService(access, sprintRepository, views).execute(new ListSprintsQuery(
                memberId, workspace.getId(), "  now ", List.of(SprintState.ACTIVE, SprintState.PLANNED), 1, 2));

        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(page.items()).extracting(view -> view.sprint().getName()).containsExactly("Now", "Next");
        assertThat(page.items().get(0)).extracting(SprintView::storyCount, SprintView::totalPoints, SprintView::donePoints)
                .containsExactly(3, 8, 5);
        assertThat(page.items().get(1).storyCount()).isZero();
    }

    @Test
    void searchesEveryStateFromTheFirstPageByDefault() {
        when(sprintRepository.search(new SprintSearchCriteria(workspace.getId(), null, EnumSet.allOf(SprintState.class), 0, 20)))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

        assertThat(new ListSprintsService(access, sprintRepository, views)
                .execute(new ListSprintsQuery(memberId, workspace.getId(), " ", null, null, null)).items()).isEmpty();
    }

    @Test
    void aMemberGetsASprint() {
        Sprint sprint = sprint(SprintState.PLANNED);

        assertThat(new GetSprintService(lookup, views).execute(new GetSprintQuery(memberId, workspace.getId(), sprint.getId())).sprint())
                .isSameAs(sprint);
    }

    @Test
    void startingRecordsTheCommittedPoints() {
        Sprint sprint = sprint(SprintState.PLANNED);
        when(sprintRepository.findActive(workspace.getId())).thenReturn(Optional.empty());
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(
                story(1, 5, ColumnKind.TODO, sprint),
                story(2, 2, ColumnKind.TODO, sprint),
                story(3, null, ColumnKind.TODO, sprint)
        ));

        SprintView view = new StartSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId()));

        assertThat(view.sprint().getState()).isEqualTo(SprintState.ACTIVE);
        assertThat(view.sprint().getCommittedPoints()).isEqualTo(7);
        assertThat(view.sprint().getStartedAt()).isNotNull();
    }

    @Test
    void onlyOneSprintIsActiveAtATime() {
        Sprint sprint = sprint(SprintState.PLANNED);
        when(sprintRepository.findActive(workspace.getId()))
                .thenReturn(Optional.of(Sprint.create(workspace.getId(), "Now", null, START, END)));

        assertThatThrownBy(() -> new StartSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId())))
                .isInstanceOf(ConflictException.class);
        assertThat(sprint.isPlanned()).isTrue();
    }

    @Test
    void anActiveOrClosedSprintCannotBeStarted() {
        StartSprintService start = new StartSprintService(lookup, sprintRepository, storyRepository, views, snapshots);

        for (SprintState state : List.of(SprintState.ACTIVE, SprintState.CLOSED)) {
            Sprint sprint = sprint(state);
            assertThatThrownBy(() -> start.execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId())))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void closingRecordsTheDonePointsAndSendsUnfinishedStoriesBackToTheBacklog() {
        Sprint sprint = sprint(SprintState.ACTIVE);
        UserStory done = story(1, 5, ColumnKind.DONE, sprint);
        UserStory doing = story(2, 3, ColumnKind.IN_PROGRESS, sprint);
        UserStory todo = story(3, 2, ColumnKind.TODO, sprint);
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(done, doing, todo));

        SprintView view = new CloseSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId()));

        assertThat(view.sprint().getState()).isEqualTo(SprintState.CLOSED);
        assertThat(view.sprint().getCompletedPoints()).isEqualTo(5);
        assertThat(view.sprint().getClosedAt()).isNotNull();
        assertThat(done.getSprintId()).isEqualTo(sprint.getId());
        assertThat(doing.getSprintId()).isNull();
        assertThat(todo.getSprintId()).isNull();
        verify(storyRepository).saveAll(List.of(doing, todo));
    }

    @Test
    void onlyTheActiveSprintCanBeClosed() {
        Sprint sprint = sprint(SprintState.PLANNED);

        assertThatThrownBy(() -> new CloseSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aClosedSprintCannotBeChanged() {
        Sprint sprint = sprint(SprintState.CLOSED);

        assertThatThrownBy(() -> new UpdateSprintService(lookup, sprintRepository, views).execute(
                new UpdateSprintCommand(ownerId, workspace.getId(), sprint.getId(), "New", null, START, END)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatesNameGoalAndDates() {
        Sprint sprint = sprint(SprintState.ACTIVE);

        SprintView view = new UpdateSprintService(lookup, sprintRepository, views).execute(
                new UpdateSprintCommand(ownerId, workspace.getId(), sprint.getId(), " New ", " Ship it ", START, END.plusDays(1)));

        assertThat(view.sprint().getName()).isEqualTo("New");
        assertThat(view.sprint().getGoal()).isEqualTo("Ship it");
        assertThat(view.sprint().getEndDate()).isEqualTo(END.plusDays(1));
    }

    @Test
    void onlyAPlannedSprintCanBeDeleted() {
        DeleteSprintService delete = new DeleteSprintService(lookup, sprintRepository);
        Sprint planned = sprint(SprintState.PLANNED);
        Sprint active = sprint(SprintState.ACTIVE);

        delete.execute(new SprintCommand(ownerId, workspace.getId(), planned.getId()));

        verify(sprintRepository).delete(planned);
        assertThatThrownBy(() -> delete.execute(new SprintCommand(ownerId, workspace.getId(), active.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aMemberPlansAStoryIntoASprintAndBackToTheBacklog() {
        Sprint sprint = sprint(SprintState.PLANNED);
        UserStory story = story(1, 3, ColumnKind.TODO, null);
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));
        PlanStoryService plan = new PlanStoryService(new StoryLookup(access, storyRepository), lookup, storyRepository);

        assertThat(plan.execute(new PlanStoryCommand(memberId, workspace.getId(), "PRU-1", sprint.getId())).getSprintId())
                .isEqualTo(sprint.getId());
        assertThat(plan.execute(new PlanStoryCommand(memberId, workspace.getId(), "PRU-1", null)).getSprintId()).isNull();
    }

    @Test
    void aStoryCannotBePlannedIntoAClosedSprint() {
        Sprint sprint = sprint(SprintState.CLOSED);
        UserStory story = story(1, 3, ColumnKind.TODO, null);
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        PlanStoryService plan = new PlanStoryService(new StoryLookup(access, storyRepository), lookup, storyRepository);

        assertThatThrownBy(() -> plan.execute(new PlanStoryCommand(memberId, workspace.getId(), "PRU-1", sprint.getId())))
                .isInstanceOf(BusinessRuleException.class);
        verify(storyRepository, never()).save(any());
    }

    private SprintSnapshot recorded() {
        ArgumentCaptor<SprintSnapshot> captor = ArgumentCaptor.forClass(SprintSnapshot.class);
        verify(snapshotRepository).saveForDay(captor.capture());
        return captor.getValue();
    }

    @Test
    void startingRecordsTheFirstSnapshotOfTheBurndown() {
        Sprint sprint = sprint(SprintState.PLANNED);
        when(sprintRepository.findActive(workspace.getId())).thenReturn(Optional.empty());
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(
                story(1, 5, ColumnKind.TODO, sprint),
                story(2, 3, ColumnKind.DONE, sprint),
                story(3, null, ColumnKind.TODO, sprint)
        ));

        new StartSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId()));

        assertThat(recorded()).extracting(SprintSnapshot::sprintId, SprintSnapshot::totalPoints, SprintSnapshot::remainingPoints,
                        SprintSnapshot::totalStories, SprintSnapshot::remainingStories, SprintSnapshot::recordedAt)
                .containsExactly(sprint.getId(), 8, 5, 3, 2, clock.instant());
    }

    @Test
    void closingRecordsTheLastSnapshotBeforeSendingStoriesBack() {
        Sprint sprint = sprint(SprintState.ACTIVE);
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(
                story(1, 5, ColumnKind.DONE, sprint),
                story(2, 3, ColumnKind.IN_PROGRESS, sprint),
                story(3, 2, ColumnKind.TODO, sprint)
        ));

        new CloseSprintService(lookup, sprintRepository, storyRepository, views, snapshots)
                .execute(new SprintCommand(ownerId, workspace.getId(), sprint.getId()));

        assertThat(recorded()).extracting(SprintSnapshot::totalPoints, SprintSnapshot::remainingPoints, SprintSnapshot::remainingStories)
                .containsExactly(10, 5, 2);
        var order = inOrder(snapshotRepository, storyRepository);
        order.verify(snapshotRepository).saveForDay(any());
        order.verify(storyRepository).saveAll(anyList());
    }

    @Test
    void aSnapshotBelongsToTheDayInTheConfiguredZone() {
        Sprint sprint = Sprint.create(workspace.getId(), "Now", null, START, END);

        snapshots.record(sprint);

        assertThat(recorded().date()).isEqualTo(LocalDate.of(2026, 12, 2));
    }

    @Test
    void theRecorderSnapshotsEveryActiveSprint() {
        Sprint one = Sprint.create(workspace.getId(), "One", null, START, END);
        one.start(0);
        Sprint two = Sprint.create(UUID.randomUUID(), "Two", null, START, END);
        two.start(0);
        when(sprintRepository.findAllActive()).thenReturn(List.of(one, two));

        new RecordSprintSnapshotsService(sprintRepository, snapshots).execute();

        verify(snapshotRepository, times(2)).saveForDay(any());
    }
}
