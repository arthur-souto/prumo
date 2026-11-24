package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.MoveStoryCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveStoryServiceTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    private final UUID userId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private UUID todo;
    private UUID progress;
    private UserStory a;
    private UserStory b;
    private UserStory c;
    private UserStory moving;
    private MoveStoryService service;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        board = BoardFixtures.boardFor(workspace);
        todo = BoardFixtures.column(board, ColumnKind.TODO).getId();
        progress = BoardFixtures.column(board, ColumnKind.IN_PROGRESS).getId();
        a = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 1, "G");
        b = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 2, "P");
        c = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 3, "Y");
        moving = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 4, "M");
        WorkspaceAccess access = new WorkspaceAccess(workspaceRepository);
        service = new MoveStoryService(
                access,
                new BoardProvisioner(boardRepository),
                new StoryLookup(access, storyRepository),
                storyRepository
        );
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-4")).thenReturn(Optional.of(moving));
        lenient().when(storyRepository.findAllByColumnId(progress)).thenReturn(List.of(a, b, c));
    }

    private MoveStoryCommand move(UUID column, UUID after, UUID before) {
        return new MoveStoryCommand(userId, workspace.getId(), "PRU-4", column, after, before);
    }

    private void saves() {
        when(storyRepository.save(moving)).thenReturn(moving);
    }

    @Test
    void movingBetweenTwoNeighboursGetsARankBetweenThem() {
        saves();

        service.execute(move(progress, a.getId(), b.getId()));

        assertThat(moving.getColumnId()).isEqualTo(progress);
        assertThat(moving.getRank()).isGreaterThan("G").isLessThan("P");
    }

    @Test
    void onlyTheMovedStoryIsWritten() {
        saves();

        service.execute(move(progress, a.getId(), b.getId()));

        verify(storyRepository).save(moving);
        verify(storyRepository, never()).saveAll(any());
    }

    @Test
    void movingAfterTheLastCardAppends() {
        saves();

        service.execute(move(progress, c.getId(), null));

        assertThat(moving.getRank()).isGreaterThan("Y");
    }

    @Test
    void movingBeforeTheFirstCardPrepends() {
        saves();

        service.execute(move(progress, null, a.getId()));

        assertThat(moving.getRank()).isLessThan("G");
    }

    @Test
    void movingWithoutNeighboursAppendsToTheColumn() {
        saves();

        service.execute(move(progress, null, null));

        assertThat(moving.getRank()).isGreaterThan("Y");
    }

    @Test
    void movingIntoAnEmptyColumnWorks() {
        saves();
        UUID review = BoardFixtures.column(board, ColumnKind.IN_REVIEW).getId();
        when(storyRepository.findAllByColumnId(review)).thenReturn(List.of());

        service.execute(move(review, null, null));

        assertThat(moving.getColumnId()).isEqualTo(review);
        assertThat(moving.getRank()).isNotBlank();
    }

    @Test
    void reordersInsideTheSameColumnIgnoringTheMovedCardItself() {
        saves();
        when(storyRepository.findAllByColumnId(todo)).thenReturn(List.of(moving));

        service.execute(move(todo, null, null));

        assertThat(moving.getColumnId()).isEqualTo(todo);
    }

    @Test
    void neighboursMustBeInTheDestinationColumn() {
        assertThatThrownBy(() -> service.execute(move(progress, UUID.randomUUID(), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(storyRepository, never()).save(any());
    }

    @Test
    void whenBothNeighboursAreGivenTheyMustBeAdjacent() {
        assertThatThrownBy(() -> service.execute(move(progress, a.getId(), c.getId())))
                .isInstanceOf(BusinessRuleException.class);
        verify(storyRepository, never()).save(any());
    }

    @Test
    void theDestinationColumnMustBelongToTheBoard() {
        assertThatThrownBy(() -> service.execute(move(UUID.randomUUID(), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void strangersCannotMove() {
        assertThatThrownBy(() -> service.execute(
                new MoveStoryCommand(UUID.randomUUID(), workspace.getId(), "PRU-4", progress, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(storyRepository, never()).save(any());
    }

    @Test
    void duplicatedRanksTriggerARebalanceOfTheColumn() {
        UserStory twin = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 5, "G");
        when(storyRepository.findAllByColumnId(progress)).thenReturn(List.of(a, twin, c));

        service.execute(move(progress, a.getId(), twin.getId()));

        ArgumentCaptor<List<UserStory>> saved = ArgumentCaptor.forClass(List.class);
        verify(storyRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(UserStory::getKey).containsExactly("PRU-1", "PRU-4", "PRU-5", "PRU-3");
        assertThat(saved.getValue()).extracting(UserStory::getRank).isSorted().doesNotHaveDuplicates();
        assertThat(moving.getColumnId()).isEqualTo(progress);
    }

    @Test
    void anExhaustedRankTriggersARebalance() {
        String prefix = "V" + "0".repeat(40);
        UserStory low = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 6, prefix + "1");
        UserStory high = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.IN_PROGRESS), 7, prefix + "2");
        when(storyRepository.findAllByColumnId(progress)).thenReturn(List.of(low, high));

        service.execute(move(progress, low.getId(), high.getId()));

        ArgumentCaptor<List<UserStory>> saved = ArgumentCaptor.forClass(List.class);
        verify(storyRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(UserStory::getKey).containsExactly("PRU-6", "PRU-4", "PRU-7");
        assertThat(saved.getValue()).extracting(story -> story.getRank().length()).allMatch(length -> length <= 3);
    }
}
