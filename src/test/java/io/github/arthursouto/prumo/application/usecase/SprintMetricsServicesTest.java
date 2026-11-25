package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.command.GetVelocityQuery;
import io.github.arthursouto.prumo.application.port.in.result.BurndownDay;
import io.github.arthursouto.prumo.application.port.in.result.SprintMetrics;
import io.github.arthursouto.prumo.application.port.in.result.Velocity;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintMetricsServicesTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 5);

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

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private WorkspaceAccess access;
    private SprintLookup lookup;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        board = BoardFixtures.boardFor(workspace);
        access = new WorkspaceAccess(workspaceRepository);
        lookup = new SprintLookup(access, sprintRepository);
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of());
    }

    private GetSprintMetricsService service(LocalDate today) {
        Clock clock = Clock.fixed(today.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        SprintSnapshots snapshots = new SprintSnapshots(storyRepository, snapshotRepository,
                new SprintViews(boardRepository, storyRepository), clock, ZONE);
        return new GetSprintMetricsService(lookup, snapshotRepository, snapshots);
    }

    private Sprint sprint(SprintState state, Integer committed, LocalDate closedOn) {
        Instant closedAt = closedOn == null ? null : closedOn.atTime(18, 0).atZone(ZONE).toInstant();
        Sprint sprint = Sprint.restore(UUID.randomUUID(), workspace.getId(), "Sprint 1", null, START, END, state,
                committed, null, committed == null ? null : Instant.now(), closedAt, Instant.now(), Instant.now());
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        return sprint;
    }

    private SprintSnapshot snapshot(Sprint sprint, LocalDate date, int total, int remaining) {
        return SprintSnapshot.of(sprint.getId(), date, total, remaining, 4, 2, Instant.now());
    }

    private UserStory story(int number, int points, ColumnKind kind, Sprint sprint) {
        return UserStory.create(workspace.getId(), number, "PRU-" + number, "Story " + number, null, StoryPriority.MEDIUM,
                points, BoardFixtures.column(board, kind).getId(), "r" + number, List.of(), List.of()).withSprint(sprint.getId());
    }

    private SprintMetrics metrics(LocalDate today, Sprint sprint) {
        return service(today).execute(new GetSprintQuery(memberId, workspace.getId(), sprint.getId()));
    }

    @Test
    void theIdealLineGoesFromTheCommittedPointsToZeroOnTheLastDay() {
        Sprint sprint = sprint(SprintState.ACTIVE, 20, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of());

        SprintMetrics metrics = metrics(START, sprint);

        assertThat(metrics.baselinePoints()).isEqualTo(20);
        assertThat(metrics.burndown()).extracting(BurndownDay::date, BurndownDay::idealPoints).containsExactly(
                tuple(START, 20.0), tuple(START.plusDays(1), 15.0), tuple(START.plusDays(2), 10.0),
                tuple(START.plusDays(3), 5.0), tuple(END, 0.0));
    }

    @Test
    void daysWithoutASnapshotRepeatTheLastOneAndDaysToComeAreEmpty() {
        Sprint sprint = sprint(SprintState.ACTIVE, 20, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of(snapshot(sprint, START, 20, 20)));
        when(storyRepository.findAllBySprintIds(anyList())).thenReturn(List.of(
                story(1, 13, ColumnKind.DONE, sprint), story(2, 8, ColumnKind.TODO, sprint)));

        SprintMetrics metrics = metrics(START.plusDays(2), sprint);

        assertThat(metrics.burndown()).extracting(BurndownDay::date, BurndownDay::totalPoints, BurndownDay::remainingPoints).containsExactly(
                tuple(START, 20, 20),
                tuple(START.plusDays(1), 20, 20),
                tuple(START.plusDays(2), 21, 8),
                tuple(START.plusDays(3), null, null),
                tuple(END, null, null));
        verify(snapshotRepository, never()).saveForDay(any());
    }

    @Test
    void anOverdueSprintRunsUntilToday() {
        Sprint sprint = sprint(SprintState.ACTIVE, 10, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of());

        SprintMetrics metrics = metrics(END.plusDays(2), sprint);

        assertThat(metrics.burndown()).hasSize(7);
        assertThat(metrics.burndown().get(6)).extracting(BurndownDay::date, BurndownDay::remainingPoints, BurndownDay::idealPoints)
                .containsExactly(END.plusDays(2), 0, 0.0);
    }

    @Test
    void aClosedSprintEndsOnTheDayItClosed() {
        Sprint sprint = sprint(SprintState.CLOSED, 20, START.plusDays(3));
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of(
                snapshot(sprint, START, 20, 20), snapshot(sprint, START.plusDays(3), 20, 4)));

        SprintMetrics metrics = metrics(LocalDate.of(2027, 1, 10), sprint);

        assertThat(metrics.burndown()).extracting(BurndownDay::remainingPoints).containsExactly(20, 20, 20, 4, null);
    }

    @Test
    void aSprintStartedEarlyCarriesItsFirstSnapshotIntoTheFirstDay() {
        Sprint sprint = sprint(SprintState.ACTIVE, 12, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of(snapshot(sprint, START.minusDays(1), 12, 12)));

        SprintMetrics metrics = metrics(START.plusDays(1), sprint);

        assertThat(metrics.burndown().get(0)).extracting(BurndownDay::date, BurndownDay::remainingPoints).containsExactly(START, 12);
    }

    @Test
    void aPlannedSprintHasOnlyTheIdealLineFromItsCurrentScope() {
        Sprint sprint = sprint(SprintState.PLANNED, null, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of());

        SprintMetrics metrics = metrics(START.minusDays(3), sprint);

        assertThat(metrics.baselinePoints()).isZero();
        assertThat(metrics.burndown()).hasSize(5).allSatisfy(day -> assertThat(day.remainingPoints()).isNull());
    }

    @Test
    void aSprintOfAnotherWorkspaceHasNoMetrics() {
        Sprint other = Sprint.create(UUID.randomUUID(), "Other", null, START, END);
        when(sprintRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> metrics(START, other)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Sprint closed(String name, int committed, int completed) {
        return Sprint.restore(UUID.randomUUID(), workspace.getId(), name, null, START, END, SprintState.CLOSED,
                committed, completed, Instant.now(), Instant.now(), Instant.now(), Instant.now());
    }

    @Test
    void velocityListsTheLatestClosedSprintsOldestFirstWithTheirAverages() {
        Sprint third = closed("Sprint 3", 20, 18);
        Sprint second = closed("Sprint 2", 15, 10);
        Sprint first = closed("Sprint 1", 10, 9);
        when(sprintRepository.search(new SprintSearchCriteria(workspace.getId(), null, Set.of(SprintState.CLOSED), 0, 3)))
                .thenReturn(new PageResult<>(List.of(third, second, first), 0, 3, 7));

        Velocity velocity = new GetVelocityService(access, sprintRepository)
                .execute(new GetVelocityQuery(memberId, workspace.getId(), 3));

        assertThat(velocity.sprints()).extracting(Sprint::getName).containsExactly("Sprint 1", "Sprint 2", "Sprint 3");
        assertThat(velocity.averageCompleted()).isEqualTo(12.3);
        assertThat(velocity.averageCommitted()).isEqualTo(15.0);
    }

    @Test
    void velocityTakesTheLatestSixByDefaultAndIsZeroWithoutClosedSprints() {
        when(sprintRepository.search(new SprintSearchCriteria(workspace.getId(), null, Set.of(SprintState.CLOSED), 0, 6)))
                .thenReturn(new PageResult<>(List.of(), 0, 6, 0));

        Velocity velocity = new GetVelocityService(access, sprintRepository)
                .execute(new GetVelocityQuery(memberId, workspace.getId(), null));

        assertThat(velocity.sprints()).isEmpty();
        assertThat(velocity.averageCompleted()).isZero();
    }

    @Test
    void aSprintStartedEmptyDrawsTheIdealLineFromItsFirstRecordedTotal() {
        Sprint sprint = sprint(SprintState.ACTIVE, 0, null);
        when(snapshotRepository.findBySprintId(sprint.getId())).thenReturn(List.of(snapshot(sprint, START, 23, 10)));

        SprintMetrics metrics = metrics(START.plusDays(1), sprint);

        assertThat(metrics.baselinePoints()).isEqualTo(23);
        assertThat(metrics.burndown().get(0).idealPoints()).isEqualTo(23.0);
    }
}
