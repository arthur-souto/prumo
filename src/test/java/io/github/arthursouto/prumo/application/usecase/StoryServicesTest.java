package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.application.port.in.command.ChecklistInput;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetStoryQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.SetChecklistItemDoneCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand;
import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryServicesTest {

    @Mock
    private ActivityRepositoryPort activityRepository;

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    private final UUID userId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private WorkspaceAccess access;
    private BoardProvisioner provisioner;
    private StoryLookup lookup;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        board = BoardFixtures.boardFor(workspace);
        access = new WorkspaceAccess(workspaceRepository);
        provisioner = new BoardProvisioner(boardRepository);
        lookup = new StoryLookup(access, storyRepository);
    }

    private CreateStoryCommand create(String title, UUID columnId) {
        return new CreateStoryCommand(
                userId,
                workspace.getId(),
                title,
                "  desc  ",
                null,
                3,
                List.of("api", "API", " ", "backend"),
                List.of(new ChecklistInput(null, "works", true), new ChecklistInput(null, "  ", false)),
                columnId
        );
    }

    private void asMember() {
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
    }

    @Test
    void createsTheStoryInTheTodoColumnWithTheNextKeyAndTheLastRank() {
        asMember();
        UserStory existing = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "V");
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        when(boardRepository.allocateStoryNumber(workspace.getId())).thenReturn(2);
        when(storyRepository.findAllByColumnId(BoardFixtures.column(board, ColumnKind.TODO).getId()))
                .thenReturn(List.of(existing));
        when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));

        UserStory created = new CreateStoryService(access, provisioner, boardRepository, storyRepository, new ActivityLog(activityRepository))
                .execute(create("  Reset  ", null));

        assertThat(created.getKey()).isEqualTo("PRU-2");
        assertThat(created.getNumber()).isEqualTo(2);
        assertThat(created.getTitle()).isEqualTo("Reset");
        assertThat(created.getDescription()).isEqualTo("desc");
        assertThat(created.getPriority()).isEqualTo(StoryPriority.MEDIUM);
        assertThat(created.getColumnId()).isEqualTo(BoardFixtures.column(board, ColumnKind.TODO).getId());
        assertThat(created.getRank()).isGreaterThan("V");
        assertThat(created.getLabels()).containsExactly("api", "backend");
        assertThat(created.getAcceptanceCriteria()).extracting(ChecklistItem::text).containsExactly("works");
        assertThat(created.getAcceptanceCriteria()).extracting(ChecklistItem::done).containsExactly(true);
    }

    @Test
    void createsInTheRequestedColumn() {
        asMember();
        UUID progress = BoardFixtures.column(board, ColumnKind.IN_PROGRESS).getId();
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        when(boardRepository.allocateStoryNumber(workspace.getId())).thenReturn(1);
        when(storyRepository.findAllByColumnId(progress)).thenReturn(List.of());
        when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));

        UserStory created = new CreateStoryService(access, provisioner, boardRepository, storyRepository, new ActivityLog(activityRepository))
                .execute(create("X", progress));

        assertThat(created.getColumnId()).isEqualTo(progress);
    }

    @Test
    void rejectsAColumnOfAnotherBoard() {
        asMember();
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> new CreateStoryService(access, provisioner, boardRepository, storyRepository, new ActivityLog(activityRepository))
                .execute(create("X", UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(boardRepository, never()).allocateStoryNumber(any());
    }

    @Test
    void strangersCannotCreateAndConsumeNoNumber() {
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        CreateStoryCommand command = new CreateStoryCommand(
                UUID.randomUUID(), workspace.getId(), "X", null, null, null, null, null, null
        );

        assertThatThrownBy(() -> new CreateStoryService(access, provisioner, boardRepository, storyRepository, new ActivityLog(activityRepository))
                .execute(command))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(boardRepository, never()).allocateStoryNumber(any());
        verify(storyRepository, never()).save(any());
    }

    @Test
    void listFiltersByPriorityLabelAndTextAndSortsNewestFirst() {
        asMember();
        UserStory a = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        UserStory b = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 2, "b");
        a.update("Login page", "about oauth", StoryPriority.HIGH, null, List.of("Frontend"), List.of());
        b.update("Reset password", null, StoryPriority.LOW, null, List.of("backend"), List.of());
        when(storyRepository.findAllByWorkspaceId(workspace.getId())).thenReturn(List.of(a, b));
        ListStoriesService service = new ListStoriesService(access, storyRepository);

        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), null, null, null)))
                .extracting(UserStory::getKey).containsExactly("PRU-2", "PRU-1");
        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), null, StoryPriority.HIGH, null)))
                .extracting(UserStory::getKey).containsExactly("PRU-1");
        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), "FRONTEND", null, null)))
                .extracting(UserStory::getKey).containsExactly("PRU-1");
        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), null, null, "OAUTH")))
                .extracting(UserStory::getKey).containsExactly("PRU-1");
        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), null, null, "pru-2")))
                .extracting(UserStory::getKey).containsExactly("PRU-2");
        assertThat(service.execute(new ListStoriesQuery(userId, workspace.getId(), "backend", StoryPriority.HIGH, null)))
                .isEmpty();
    }

    @Test
    void getFindsByKeyIgnoringCaseAndReportsMissingOnes() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 7, "a");
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-7")).thenReturn(Optional.of(story));
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-8")).thenReturn(Optional.empty());
        GetStoryService service = new GetStoryService(access, storyRepository);

        assertThat(service.execute(new GetStoryQuery(userId, workspace.getId(), "pru-7"))).isSameAs(story);
        assertThatThrownBy(() -> service.execute(new GetStoryQuery(userId, workspace.getId(), "pru-8")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateKeepsKnownChecklistIdsAndDropsMissingItems() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        ChecklistItem kept = ChecklistItem.create("keep", false);
        ChecklistItem removed = ChecklistItem.create("remove", false);
        story.update("T", null, StoryPriority.LOW, null, List.of(), List.of(kept, removed));
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);

        new UpdateStoryService(lookup, storyRepository, new ActivityLog(activityRepository)).execute(new UpdateStoryCommand(
                userId,
                workspace.getId(),
                "PRU-1",
                " New title ",
                "",
                StoryPriority.URGENT,
                8,
                List.of("x"),
                List.of(new ChecklistInput(kept.id(), "keep edited", true), new ChecklistInput(UUID.randomUUID(), "new", false))
        ));

        assertThat(story.getTitle()).isEqualTo("New title");
        assertThat(story.getDescription()).isNull();
        assertThat(story.getPriority()).isEqualTo(StoryPriority.URGENT);
        assertThat(story.getAcceptanceCriteria()).hasSize(2);
        assertThat(story.getAcceptanceCriteria().get(0)).isEqualTo(new ChecklistItem(kept.id(), "keep edited", true));
        assertThat(story.getAcceptanceCriteria()).extracting(ChecklistItem::id).doesNotContain(removed.id());
    }

    @Test
    void tickingAnItemSavesTheStoryAndAnUnknownItemIsNotFound() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        ChecklistItem item = ChecklistItem.create("x", false);
        story.update("T", null, StoryPriority.LOW, null, List.of(), List.of(item));
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);
        SetChecklistItemDoneService service = new SetChecklistItemDoneService(lookup, storyRepository);

        service.execute(new SetChecklistItemDoneCommand(userId, workspace.getId(), "PRU-1", item.id(), true));

        assertThat(story.getAcceptanceCriteria().get(0).done()).isTrue();
        assertThatThrownBy(() -> service.execute(
                new SetChecklistItemDoneCommand(userId, workspace.getId(), "PRU-1", UUID.randomUUID(), true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesByIdAndStrangersCannotReachTheStory() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        DeleteStoryService service = new DeleteStoryService(lookup, storyRepository);

        service.execute(new DeleteStoryCommand(userId, workspace.getId(), "pru-1"));

        ArgumentCaptor<UUID> deleted = ArgumentCaptor.forClass(UUID.class);
        verify(storyRepository).deleteById(deleted.capture());
        assertThat(deleted.getValue()).isEqualTo(story.getId());
        assertThatThrownBy(() -> service.execute(new DeleteStoryCommand(UUID.randomUUID(), workspace.getId(), "PRU-1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void creatingAStoryLeavesAHistoryEntryByTheUser() {
        asMember();
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        when(boardRepository.allocateStoryNumber(workspace.getId())).thenReturn(2);
        when(storyRepository.findAllByColumnId(BoardFixtures.column(board, ColumnKind.TODO).getId())).thenReturn(List.of());
        when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));

        UserStory created = new CreateStoryService(access, provisioner, boardRepository, storyRepository, new ActivityLog(activityRepository))
                .execute(create("Reset", null));

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(saved.capture());
        assertThat(saved.getValue().type()).isEqualTo(ActivityType.STORY_CREATED);
        assertThat(saved.getValue().storyId()).isEqualTo(created.getId());
        assertThat(saved.getValue().actorId()).isEqualTo(userId);
        assertThat(saved.getValue().toValue()).isEqualTo("Reset");
    }

    private UpdateStoryCommand editing(String title, StoryPriority priority) {
        return new UpdateStoryCommand(userId, workspace.getId(), "PRU-1", title, null, priority, null, List.of(), List.of());
    }

    @Test
    void anUpdateRecordsWhichFieldsItChangedAndNothingElse() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        story.update("Old", null, StoryPriority.LOW, null, List.of(), List.of());
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);

        new UpdateStoryService(lookup, storyRepository, new ActivityLog(activityRepository)).execute(editing("New", StoryPriority.HIGH));

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(saved.capture());
        assertThat(saved.getValue().type()).isEqualTo(ActivityType.STORY_UPDATED);
        assertThat(saved.getValue().toValue()).isEqualTo("title,priority");
        assertThat(saved.getValue().actorId()).isEqualTo(userId);
    }

    @Test
    void savingWithoutChangingAnythingLeavesNoHistory() {
        asMember();
        UserStory story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        story.update("Same", null, StoryPriority.LOW, null, List.of(), List.of());
        when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);

        new UpdateStoryService(lookup, storyRepository, new ActivityLog(activityRepository)).execute(editing("Same", StoryPriority.LOW));

        verify(activityRepository, never()).save(any());
    }
}
