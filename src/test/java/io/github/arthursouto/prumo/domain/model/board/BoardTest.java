package io.github.arthursouto.prumo.domain.model.board;

import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = Board.withDefaults(UUID.randomUUID(), "PRU");
    }

    @Test
    void newBoardsHaveTheFourDefaultColumnsInOrder() {
        assertThat(board.getColumns()).extracting(BoardColumn::getName)
                .containsExactly("To Do", "In Progress", "In Review", "Done");
        assertThat(board.getColumns()).extracting(BoardColumn::getKind)
                .containsExactly(ColumnKind.TODO, ColumnKind.IN_PROGRESS, ColumnKind.IN_REVIEW, ColumnKind.DONE);
        assertThat(board.isAutomationEnabled()).isFalse();
        assertThat(board.getKeyPrefix()).isEqualTo("PRU");
    }

    @Test
    void addedColumnsGoToTheEndAsCustom() {
        BoardColumn column = board.addColumn("QA");

        assertThat(column.getKind()).isEqualTo(ColumnKind.CUSTOM);
        assertThat(column.getPosition()).isEqualTo(4);
        assertThat(board.getColumns()).last().isEqualTo(column);
    }

    @Test
    void columnNamesAreUniqueIgnoringCase() {
        assertThatThrownBy(() -> board.addColumn(" done "))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void renamingKeepsTheKindSoAutomationStillWorks() {
        BoardColumn done = board.ofKind(ColumnKind.DONE).orElseThrow();

        board.rename(done.getId(), "Shipped");

        assertThat(board.ofKind(ColumnKind.DONE)).get().extracting(BoardColumn::getName).isEqualTo("Shipped");
    }

    @Test
    void renamingToAnotherColumnsNameConflicts() {
        BoardColumn done = board.ofKind(ColumnKind.DONE).orElseThrow();

        assertThatThrownBy(() -> board.rename(done.getId(), "to do")).isInstanceOf(ConflictException.class);
    }

    @Test
    void renamingToItsOwnNameWithAnotherCaseIsAllowed() {
        BoardColumn done = board.ofKind(ColumnKind.DONE).orElseThrow();

        board.rename(done.getId(), "DONE");

        assertThat(done.getName()).isEqualTo("DONE");
    }

    @Test
    void unknownColumnsAreNotFound() {
        assertThatThrownBy(() -> board.rename(UUID.randomUUID(), "X")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reorderingRenumbersEveryColumn() {
        List<UUID> reversed = board.getColumns().stream().map(BoardColumn::getId).toList().reversed();

        board.reorder(reversed);

        assertThat(board.getColumns()).extracting(BoardColumn::getId).containsExactlyElementsOf(reversed);
        assertThat(board.getColumns()).extracting(BoardColumn::getPosition).containsExactly(0, 1, 2, 3);
    }

    @Test
    void reorderingRequiresEveryColumnExactlyOnce() {
        List<UUID> ids = board.getColumns().stream().map(BoardColumn::getId).toList();

        assertThatThrownBy(() -> board.reorder(ids.subList(0, 3))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> board.reorder(List.of(ids.get(0), ids.get(0), ids.get(1), ids.get(2))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removingAnEmptyColumnRenumbersTheRest() {
        BoardColumn review = board.ofKind(ColumnKind.IN_REVIEW).orElseThrow();

        board.remove(review.getId(), false, null);

        assertThat(board.getColumns()).extracting(BoardColumn::getPosition).containsExactly(0, 1, 2);
        assertThat(board.find(review.getId())).isEmpty();
    }

    @Test
    void removingAColumnWithStoriesNeedsADestination() {
        BoardColumn todo = board.first();

        assertThatThrownBy(() -> board.remove(todo.getId(), true, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removingAColumnWithStoriesAcceptsAnotherColumnAsDestination() {
        BoardColumn todo = board.first();
        BoardColumn progress = board.getColumns().get(1);

        board.remove(todo.getId(), true, progress.getId());

        assertThat(board.find(todo.getId())).isEmpty();
    }

    @Test
    void theDestinationMustExistAndDifferFromTheRemovedColumn() {
        BoardColumn todo = board.first();

        assertThatThrownBy(() -> board.remove(todo.getId(), true, todo.getId())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> board.remove(todo.getId(), true, UUID.randomUUID())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void theLastColumnCannotBeRemoved() {
        Board single = Board.of(
                UUID.randomUUID(),
                "ONE",
                false,
                List.of(BoardColumn.create(UUID.randomUUID(), "Only", ColumnKind.CUSTOM, 0))
        );

        assertThatThrownBy(() -> single.remove(single.first().getId(), false, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void settingsCanChange() {
        board.changeKeyPrefix("ABC");
        board.setAutomationEnabled(true);

        assertThat(board.getKeyPrefix()).isEqualTo("ABC");
        assertThat(board.isAutomationEnabled()).isTrue();
    }
}
