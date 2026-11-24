package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import org.mockito.ArgumentCaptor;
import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.in.command.ChangeSubtaskStatusCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskBranchCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetSubtaskQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListSubtasksQuery;
import io.github.arthursouto.prumo.application.port.in.command.SuggestSubtaskBranchQuery;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskServicesTest {

    @Mock
    private ActivityRepositoryPort activityRepository;

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private SubtaskRepositoryPort subtaskRepository;

    @Mock
    private ConnectedRepositoryPort repositoryPort;

    @Mock
    private GithubConnectionRepositoryPort connectionRepository;

    @Mock
    private TokenCipherPort cipher;

    @Mock
    private GithubPort github;

    private final UUID userId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private UserStory story;
    private ConnectedRepository repository;
    private StoryLookup stories;
    private SubtaskLookup lookup;
    private BoardProvisioner provisioner;
    private GithubTokens tokens;
    private SubtaskWorkflow workflow;
    private final List<Subtask> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        board = BoardFixtures.boardFor(workspace);
        story = BoardFixtures.story(workspace, board.first(), 12, "V");
        repository = ConnectedRepository.connect(workspace.getId(), "arthur", "prumo", "main", "https://github.com/arthur/prumo", userId);
        WorkspaceAccess access = new WorkspaceAccess(workspaceRepository);
        stories = new StoryLookup(access, storyRepository);
        lookup = new SubtaskLookup(access, subtaskRepository, storyRepository);
        provisioner = new BoardProvisioner(boardRepository);
        tokens = new GithubTokens(connectionRepository, cipher);
        workflow = new SubtaskWorkflow(subtaskRepository, storyRepository, new ActivityLog(activityRepository));

        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-12")).thenReturn(Optional.of(story));
        lenient().when(storyRepository.findById(story.getId())).thenReturn(Optional.of(story));
        lenient().when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(storyRepository.findAllByColumnId(any())).thenReturn(List.of());
        lenient().when(subtaskRepository.save(any(Subtask.class))).thenAnswer(call -> {
            Subtask subtask = call.getArgument(0);
            if (!stored.contains(subtask)) {
                stored.add(subtask);
            }
            return subtask;
        });
        lenient().when(subtaskRepository.findAllByStoryId(story.getId())).thenAnswer(call -> List.copyOf(stored));
        lenient().when(boardRepository.allocateStoryNumber(workspace.getId())).thenReturn(13, 14, 15);
        lenient().when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(new GithubConnection(userId, "cipher", "arthur", Instant.now())));
        lenient().when(cipher.decrypt("cipher")).thenReturn("ghp_plain");
        lenient().when(repositoryPort.findById(repository.getId())).thenReturn(Optional.of(repository));
    }

    private CreateSubtaskService creator() {
        return new CreateSubtaskService(stories, lookup, provisioner, boardRepository, subtaskRepository, repositoryPort, tokens, github, new ActivityLog(activityRepository));
    }

    private CreateSubtaskCommand create(String title, boolean branch, String branchName) {
        return new CreateSubtaskCommand(userId, workspace.getId(), "pru-12", title, " body ", branch, branchName);
    }

    private void linkRepository() {
        story.linkRepository(repository.getId());
    }

    private Subtask existing(int number, SubtaskStatus status) {
        Subtask subtask = Subtask.create(story.getId(), workspace.getId(), number, "PRU-" + number, "Task " + number, null, stored.size());
        subtask.changeStatus(status);
        stored.add(subtask);
        lenient().when(subtaskRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-" + number)).thenReturn(Optional.of(subtask));

        return subtask;
    }

    @Test
    void aSubtaskTakesTheNextNumberOfTheWorkspaceSequenceAndTheLastPosition() {
        SubtaskView first = creator().execute(create(" Write the endpoint ", false, null));
        SubtaskView second = creator().execute(create("Test it", false, null));

        assertThat(first.subtask().getKey()).isEqualTo("PRU-13");
        assertThat(first.subtask().getNumber()).isEqualTo(13);
        assertThat(first.subtask().getTitle()).isEqualTo("Write the endpoint");
        assertThat(first.subtask().getDescription()).isEqualTo("body");
        assertThat(first.subtask().getStatus()).isEqualTo(SubtaskStatus.TO_DO);
        assertThat(first.subtask().getBranchName()).isNull();
        assertThat(first.subtask().getPosition()).isZero();
        assertThat(second.subtask().getKey()).isEqualTo("PRU-14");
        assertThat(second.subtask().getPosition()).isEqualTo(1);
        assertThat(first.storyKey()).isEqualTo("PRU-12");
        verify(github, never()).createBranch(any(), any(), any(), any(), any());
    }

    @Test
    void creatingAStoryNoLongerNeedsARepositoryButASubtaskWithABranchDoes() {
        assertThatThrownBy(() -> creator().execute(create("Task", true, "feature/PRU-13-task")))
                .isInstanceOf(BusinessRuleException.class);

        verify(boardRepository, never()).allocateStoryNumber(any());
        verify(github, never()).createBranch(any(), any(), any(), any(), any());
        verify(subtaskRepository, never()).save(any());
    }

    @Test
    void withABranchItIsCreatedFromTheDefaultBranchAndKeptOnTheSubtask() {
        linkRepository();

        SubtaskView view = creator().execute(create("Write the endpoint", true, " feature/PRU-13-write-the-endpoint "));

        verify(github).createBranch("ghp_plain", "arthur", "prumo", "feature/PRU-13-write-the-endpoint", "main");
        assertThat(view.subtask().getBranchName()).isEqualTo("feature/PRU-13-write-the-endpoint");
    }

    @Test
    void aBranchRequestNeedsAName() {
        linkRepository();

        assertThatThrownBy(() -> creator().execute(create("Task", true, "  ")))
                .isInstanceOf(BusinessRuleException.class);

        verify(github, never()).createBranch(any(), any(), any(), any(), any());
    }

    @Test
    void whenGithubFailsNoSubtaskIsLeftBehind() {
        linkRepository();
        doThrow(new GithubIntegrationException(GithubFailure.BRANCH_EXISTS, "exists"))
                .when(github).createBranch(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> creator().execute(create("Task", true, "feature/PRU-13-task")))
                .isInstanceOf(GithubIntegrationException.class);

        verify(subtaskRepository, never()).save(any());
    }

    @Test
    void theBranchCanBeCreatedLaterButOnlyOnce() {
        linkRepository();
        existing(13, SubtaskStatus.TO_DO);
        CreateSubtaskBranchService service = new CreateSubtaskBranchService(lookup, subtaskRepository, repositoryPort, tokens, github);

        SubtaskView view = service.execute(new CreateSubtaskBranchCommand(userId, workspace.getId(), "pru-13", " feature/PRU-13-task "));

        verify(github).createBranch("ghp_plain", "arthur", "prumo", "feature/PRU-13-task", "main");
        assertThat(view.subtask().getBranchName()).isEqualTo("feature/PRU-13-task");
        assertThatThrownBy(() -> service.execute(new CreateSubtaskBranchCommand(userId, workspace.getId(), "PRU-13", "feature/other")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aLaterBranchNeedsTheStoryToHaveARepository() {
        existing(13, SubtaskStatus.TO_DO);
        CreateSubtaskBranchService service = new CreateSubtaskBranchService(lookup, subtaskRepository, repositoryPort, tokens, github);

        assertThatThrownBy(() -> service.execute(new CreateSubtaskBranchCommand(userId, workspace.getId(), "PRU-13", "feature/x")))
                .isInstanceOf(BusinessRuleException.class);

        verify(github, never()).createBranch(any(), any(), any(), any(), any());
    }

    @Test
    void theBranchNameIsSuggestedFromTheSubtaskKeyAndTitle() {
        existing(13, SubtaskStatus.TO_DO);

        String name = new SuggestSubtaskBranchService(lookup)
                .execute(new SuggestSubtaskBranchQuery(userId, workspace.getId(), "PRU-13"));

        assertThat(name).isEqualTo("feature/PRU-13-task-13");
    }

    @Test
    void listingAndGettingShowOnlyThePullRequestsOfEachSubtask() {
        Subtask one = existing(13, SubtaskStatus.TO_DO);
        existing(14, SubtaskStatus.TO_DO);
        PullRequest forOne = PullRequest.track(story.getId(), UUID.randomUUID(), 7, "PR", PullRequestState.OPEN, "https://x/7", "b", Instant.now())
                .forSubtask(one.getId());
        PullRequest forStory = PullRequest.track(story.getId(), UUID.randomUUID(), 8, "PR", PullRequestState.OPEN, "https://x/8", "b", Instant.now());
        UserStory withPrs = UserStory.restore(story.getId(), workspace.getId(), 12, "PRU-12", "T", null, story.getPriority(), null,
                story.getColumnId(), "V", List.of(), List.of(), null, List.of(forOne, forStory), Instant.now(), Instant.now());
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-12")).thenReturn(Optional.of(withPrs));
        when(storyRepository.findById(story.getId())).thenReturn(Optional.of(withPrs));

        List<SubtaskView> listed = new ListSubtasksService(stories, lookup, subtaskRepository)
                .execute(new ListSubtasksQuery(userId, workspace.getId(), "pru-12"));
        SubtaskView got = new GetSubtaskService(lookup).execute(new GetSubtaskQuery(userId, workspace.getId(), "PRU-13"));

        assertThat(listed).extracting(view -> view.subtask().getKey()).containsExactly("PRU-13", "PRU-14");
        assertThat(listed.get(0).pullRequests()).extracting(PullRequest::number).containsExactly(7);
        assertThat(listed.get(1).pullRequests()).isEmpty();
        assertThat(got.pullRequests()).extracting(PullRequest::number).containsExactly(7);
    }

    @Test
    void updateAndDeleteWork() {
        Subtask subtask = existing(13, SubtaskStatus.TO_DO);

        SubtaskView updated = new UpdateSubtaskService(lookup, subtaskRepository)
                .execute(new UpdateSubtaskCommand(userId, workspace.getId(), "PRU-13", " New ", " "));
        new DeleteSubtaskService(lookup, subtaskRepository).execute(new DeleteSubtaskCommand(userId, workspace.getId(), "PRU-13"));

        assertThat(updated.subtask().getTitle()).isEqualTo("New");
        assertThat(updated.subtask().getDescription()).isNull();
        verify(subtaskRepository).deleteById(subtask.getId());
    }

    @Test
    void strangersCannotReachSubtasks() {
        existing(13, SubtaskStatus.TO_DO);
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> new GetSubtaskService(lookup).execute(new GetSubtaskQuery(stranger, workspace.getId(), "PRU-13")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> new DeleteSubtaskService(lookup, subtaskRepository)
                .execute(new DeleteSubtaskCommand(stranger, workspace.getId(), "PRU-13")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> new GetSubtaskService(lookup).execute(new GetSubtaskQuery(userId, workspace.getId(), "PRU-99")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ChangeSubtaskStatusService statusChanger() {
        return new ChangeSubtaskStatusService(lookup, provisioner, workflow);
    }

    private void changeStatus(String key, SubtaskStatus status) {
        statusChanger().execute(new ChangeSubtaskStatusCommand(userId, workspace.getId(), key, status));
    }

    @Test
    void statusChangesAreIndependentOfTheBoardColumns() {
        Subtask subtask = existing(13, SubtaskStatus.TO_DO);

        changeStatus("PRU-13", SubtaskStatus.IN_REVIEW);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.IN_REVIEW);
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any(UserStory.class));
    }

    @Test
    void aChangeMadeOnTheBoardTakesTheStatusBackFromGithubToTheUser() {
        Subtask subtask = existing(13, SubtaskStatus.TO_DO);
        subtask.changeStatus(SubtaskStatus.IN_REVIEW, ChangeSource.GITHUB);

        changeStatus("PRU-13", SubtaskStatus.DONE);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.DONE);
        assertThat(subtask.getStatusSource()).isEqualTo(ChangeSource.USER);
    }

    @Test
    void theStoryGoesToDoneWhenTheLastSubtaskIsDoneAndConsolidationIsOn() {
        board.setConsolidateStories(true);
        existing(13, SubtaskStatus.DONE);
        existing(14, SubtaskStatus.IN_REVIEW);

        changeStatus("PRU-14", SubtaskStatus.DONE);

        assertThat(story.getColumnId()).isEqualTo(board.ofKind(ColumnKind.DONE).orElseThrow().getId());
        verify(storyRepository).save(story);
    }

    @Test
    void consolidationWaitsForEverySubtaskAndIgnoresStoriesWithoutAny() {
        board.setConsolidateStories(true);
        existing(13, SubtaskStatus.TO_DO);
        existing(14, SubtaskStatus.IN_REVIEW);

        changeStatus("PRU-14", SubtaskStatus.DONE);

        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any(UserStory.class));
    }

    @Test
    void withConsolidationOffTheStoryStaysWhereItIs() {
        existing(13, SubtaskStatus.DONE);
        existing(14, SubtaskStatus.IN_REVIEW);

        changeStatus("PRU-14", SubtaskStatus.DONE);

        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any(UserStory.class));
    }

    @Test
    void aManualMoveOfTheStoryIsNeverUndoneByLaterSubtaskChanges() {
        board.setConsolidateStories(true);
        existing(13, SubtaskStatus.DONE);
        existing(14, SubtaskStatus.DONE);
        // the user pulled the story back to In Progress by hand after everything was done
        story.moveTo(board.ofKind(ColumnKind.IN_PROGRESS).orElseThrow().getId(), "W");

        changeStatus("PRU-14", SubtaskStatus.IN_REVIEW);
        changeStatus("PRU-14", SubtaskStatus.IN_PROGRESS);

        assertThat(story.getColumnId()).isEqualTo(board.ofKind(ColumnKind.IN_PROGRESS).orElseThrow().getId());
        verify(storyRepository, never()).save(any(UserStory.class));
    }

    @Test
    void creatingASubtaskAndChangingItsStatusLeaveHistoryByTheUser() {
        SubtaskView created = creator().execute(create("Write it", false, null));
        String key = created.subtask().getKey();
        lenient().when(subtaskRepository.findByWorkspaceIdAndKey(workspace.getId(), key)).thenReturn(Optional.of(created.subtask()));

        changeStatus(key, SubtaskStatus.IN_REVIEW);
        changeStatus(key, SubtaskStatus.IN_REVIEW);

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Activity::type)
                .containsExactly(ActivityType.SUBTASK_ADDED, ActivityType.SUBTASK_STATUS_CHANGED);
        assertThat(saved.getAllValues()).extracting(Activity::subtaskKey).containsOnly(key);
        assertThat(saved.getAllValues()).extracting(Activity::actorId).containsOnly(userId);
        assertThat(saved.getAllValues().get(1).fromValue()).isEqualTo("TO_DO");
        assertThat(saved.getAllValues().get(1).toValue()).isEqualTo("IN_REVIEW");
    }
}
