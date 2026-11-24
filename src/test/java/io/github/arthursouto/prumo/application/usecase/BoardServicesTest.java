package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.BoardAlreadyExistsException;
import io.github.arthursouto.prumo.application.exceptions.KeyPrefixTakenException;
import io.github.arthursouto.prumo.application.port.in.command.CreateColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetBoardQuery;
import io.github.arthursouto.prumo.application.port.in.command.RenameColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.ReorderColumnsCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateBoardSettingsCommand;
import io.github.arthursouto.prumo.application.port.in.result.BoardView;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.SubtaskProgress;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServicesTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private SubtaskRepositoryPort subtaskRepository;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private WorkspaceAccess access;
    private BoardProvisioner provisioner;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        board = BoardFixtures.boardFor(workspace);
        access = new WorkspaceAccess(workspaceRepository);
        provisioner = new BoardProvisioner(boardRepository);
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(boardRepository.save(any(Board.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void firstAccessCreatesTheBoardWithAPrefixDerivedFromTheName() {
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.empty());
        when(boardRepository.existsByKeyPrefix("PRU")).thenReturn(false);

        Board created = provisioner.ensure(workspace);

        assertThat(created.getKeyPrefix()).isEqualTo("PRU");
        assertThat(created.getColumns()).hasSize(4);
    }

    @Test
    void aTakenPrefixGetsANumericSuffix() {
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.empty());
        when(boardRepository.existsByKeyPrefix("PRU")).thenReturn(true);
        when(boardRepository.existsByKeyPrefix("PRU2")).thenReturn(true);
        when(boardRepository.existsByKeyPrefix("PRU3")).thenReturn(false);

        assertThat(provisioner.ensure(workspace).getKeyPrefix()).isEqualTo("PRU3");
    }

    @Test
    void aPrefixRaceIsRetried() {
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.empty());
        when(boardRepository.existsByKeyPrefix(any())).thenReturn(false);
        when(boardRepository.save(any(Board.class)))
                .thenThrow(new KeyPrefixTakenException())
                .thenAnswer(call -> call.getArgument(0));

        assertThat(provisioner.ensure(workspace)).isNotNull();
        verify(boardRepository, org.mockito.Mockito.times(2)).save(any(Board.class));
    }

    @Test
    void persistentPrefixRacesEndInAConflict() {
        when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.empty());
        when(boardRepository.existsByKeyPrefix(any())).thenReturn(false);
        when(boardRepository.save(any(Board.class))).thenThrow(new KeyPrefixTakenException());

        assertThatThrownBy(() -> provisioner.ensure(workspace)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aConcurrentFirstAccessReusesTheBoardThatWon() {
        when(boardRepository.findByWorkspaceId(workspace.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(board));
        when(boardRepository.existsByKeyPrefix(any())).thenReturn(false);
        when(boardRepository.save(any(Board.class))).thenThrow(new BoardAlreadyExistsException());

        assertThat(provisioner.ensure(workspace)).isSameAs(board);
    }

    @Test
    void getBoardReturnsColumnsAndStoriesForMembersOnly() {
        UserStory story = BoardFixtures.story(workspace, board.first(), 1, "V");
        when(storyRepository.findAllByWorkspaceId(workspace.getId())).thenReturn(List.of(story));
        when(subtaskRepository.progressByStoryIds(List.of(story.getId())))
                .thenReturn(java.util.Map.of(story.getId(), new SubtaskProgress(3, 1)));
        GetBoardService service = new GetBoardService(access, provisioner, storyRepository, subtaskRepository);

        BoardView view = service.execute(new GetBoardQuery(memberId, workspace.getId()));

        assertThat(view.board()).isSameAs(board);
        assertThat(view.stories()).containsExactly(story);
        assertThat(view.subtaskProgress().get(story.getId())).isEqualTo(new SubtaskProgress(3, 1));
        assertThatThrownBy(() -> service.execute(new GetBoardQuery(UUID.randomUUID(), workspace.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownersCanTurnStoryConsolidationOnAndOffAndOmittingItKeepsIt() {
        when(boardRepository.save(any(Board.class))).thenAnswer(call -> call.getArgument(0));
        UpdateBoardSettingsService service = new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository);

        assertThat(board.isConsolidateStories()).isFalse();
        assertThat(service.execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), null, null, true)).isConsolidateStories()).isTrue();
        assertThat(service.execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), null, true, null)).isConsolidateStories()).isTrue();
        assertThat(service.execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), null, null, false)).isConsolidateStories()).isFalse();
    }

    @Test
    void plainMembersCannotChangeTheBoardStructure() {
        assertThatThrownBy(() -> new CreateColumnService(access, provisioner, boardRepository)
                .execute(new CreateColumnCommand(memberId, workspace.getId(), "QA")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(memberId, workspace.getId(), null, true)))
                .isInstanceOf(ForbiddenException.class);
        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void ownersCreateRenameAndReorderColumns() {
        BoardColumn created = new CreateColumnService(access, provisioner, boardRepository)
                .execute(new CreateColumnCommand(ownerId, workspace.getId(), "  QA  "));
        assertThat(created.getName()).isEqualTo("QA");

        BoardColumn renamed = new RenameColumnService(access, provisioner, boardRepository)
                .execute(new RenameColumnCommand(ownerId, workspace.getId(), created.getId(), "QA Review"));
        assertThat(renamed.getName()).isEqualTo("QA Review");

        List<UUID> reversed = board.getColumns().stream().map(BoardColumn::getId).toList().reversed();
        Board reordered = new ReorderColumnsService(access, provisioner, boardRepository)
                .execute(new ReorderColumnsCommand(ownerId, workspace.getId(), reversed));
        assertThat(reordered.getColumns()).extracting(BoardColumn::getId).containsExactlyElementsOf(reversed);
    }

    @Test
    void settingsToggleAutomationAndChangeThePrefixWhileThereAreNoStories() {
        when(storyRepository.countByWorkspaceId(workspace.getId())).thenReturn(0L);
        when(boardRepository.existsByKeyPrefix("ABC")).thenReturn(false);

        Board updated = new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), " abc ", true));

        assertThat(updated.getKeyPrefix()).isEqualTo("ABC");
        assertThat(updated.isAutomationEnabled()).isTrue();
    }

    @Test
    void thePrefixIsLockedOnceStoriesExist() {
        when(storyRepository.countByWorkspaceId(workspace.getId())).thenReturn(3L);

        assertThatThrownBy(() -> new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), "NEW", null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void keepingTheSamePrefixIsAlwaysAllowed() {
        Board updated = new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), "pru", false));

        assertThat(updated.getKeyPrefix()).isEqualTo("PRU");
    }

    @Test
    void aPrefixUsedElsewhereConflicts() {
        when(storyRepository.countByWorkspaceId(workspace.getId())).thenReturn(0L);
        when(boardRepository.existsByKeyPrefix("TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), "TAKEN", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aRacedPrefixIsReportedAsAConflict() {
        when(storyRepository.countByWorkspaceId(workspace.getId())).thenReturn(0L);
        when(boardRepository.existsByKeyPrefix("RACE")).thenReturn(false);
        when(boardRepository.save(any(Board.class))).thenThrow(new KeyPrefixTakenException());

        assertThatThrownBy(() -> new UpdateBoardSettingsService(access, provisioner, boardRepository, storyRepository)
                .execute(new UpdateBoardSettingsCommand(ownerId, workspace.getId(), "RACE", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingAnEmptyColumnRemovesItAndSavesTheBoard() {
        BoardColumn review = BoardFixtures.column(board, ColumnKind.IN_REVIEW);
        when(storyRepository.findAllByColumnId(review.getId())).thenReturn(List.of());

        new DeleteColumnService(access, provisioner, boardRepository, storyRepository)
                .execute(new DeleteColumnCommand(ownerId, workspace.getId(), review.getId(), null));

        verify(boardRepository).deleteColumn(review.getId());
        assertThat(board.find(review.getId())).isEmpty();
        verify(storyRepository, never()).saveAll(any());
    }

    @Test
    void deletingAColumnWithStoriesWithoutADestinationIsRejected() {
        BoardColumn todo = board.first();
        when(storyRepository.findAllByColumnId(todo.getId()))
                .thenReturn(List.of(BoardFixtures.story(workspace, todo, 1, "V")));

        assertThatThrownBy(() -> new DeleteColumnService(access, provisioner, boardRepository, storyRepository)
                .execute(new DeleteColumnCommand(ownerId, workspace.getId(), todo.getId(), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(boardRepository, never()).deleteColumn(any());
    }

    @Test
    void storiesMoveToTheEndOfTheDestinationBeforeTheColumnIsDeleted() {
        BoardColumn todo = board.first();
        BoardColumn progress = BoardFixtures.column(board, ColumnKind.IN_PROGRESS);
        UserStory moved = BoardFixtures.story(workspace, todo, 1, "V");
        UserStory existing = BoardFixtures.story(workspace, progress, 2, "k");
        when(storyRepository.findAllByColumnId(todo.getId())).thenReturn(List.of(moved));
        when(storyRepository.findAllByColumnId(progress.getId())).thenReturn(List.of(existing));

        new DeleteColumnService(access, provisioner, boardRepository, storyRepository)
                .execute(new DeleteColumnCommand(ownerId, workspace.getId(), todo.getId(), progress.getId()));

        ArgumentCaptor<List<UserStory>> saved = ArgumentCaptor.forClass(List.class);
        var order = inOrder(storyRepository, boardRepository);
        order.verify(storyRepository).saveAll(saved.capture());
        order.verify(boardRepository).deleteColumn(todo.getId());
        assertThat(moved.getColumnId()).isEqualTo(progress.getId());
        assertThat(moved.getRank()).isGreaterThan("k");
    }
}
