package io.github.arthursouto.prumo.domain.model.story;

import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtaskTest {

    private Subtask subtask;

    @BeforeEach
    void setUp() {
        subtask = subtask(13, 0);
    }

    private static Subtask subtask(int number, int position) {
        return Subtask.create(UUID.randomUUID(), UUID.randomUUID(), number, "PRU-" + number, "Write the endpoint", null, position);
    }

    @Test
    void startsToDoWithoutBranchAndWithItsOwnKey() {
        assertThat(subtask.getKey()).isEqualTo("PRU-13");
        assertThat(subtask.getNumber()).isEqualTo(13);
        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.TO_DO);
        assertThat(subtask.getBranchName()).isNull();
        assertThat(subtask.getCreatedAt()).isEqualTo(subtask.getUpdatedAt());
    }

    @Test
    void anyStatusCanFollowAnyOtherAndRepeatingOneIsANoOp() throws InterruptedException {
        Thread.sleep(2);

        assertThat(subtask.changeStatus(SubtaskStatus.IN_PROGRESS)).isTrue();
        assertThat(subtask.changeStatus(SubtaskStatus.IN_REVIEW)).isTrue();
        assertThat(subtask.changeStatus(SubtaskStatus.DONE)).isTrue();
        assertThat(subtask.changeStatus(SubtaskStatus.TO_DO)).isTrue();
        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.TO_DO);
        assertThat(subtask.getUpdatedAt()).isAfter(subtask.getCreatedAt());

        assertThat(subtask.changeStatus(SubtaskStatus.TO_DO)).isFalse();
    }

    @Test
    void aSubtaskHasAtMostOneBranch() {
        subtask.recordBranch("feature/PRU-13-write-the-endpoint");

        assertThat(subtask.getBranchName()).isEqualTo("feature/PRU-13-write-the-endpoint");
        assertThatThrownBy(() -> subtask.recordBranch("feature/another"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(subtask.getBranchName()).isEqualTo("feature/PRU-13-write-the-endpoint");
    }

    @Test
    void updateReplacesTitleAndDescription() {
        subtask.update("New title", "# details");

        assertThat(subtask.getTitle()).isEqualTo("New title");
        assertThat(subtask.getDescription()).isEqualTo("# details");
        assertThat(subtask.getKey()).isEqualTo("PRU-13");
    }

    @Test
    void moveChangesThePosition() {
        subtask.moveTo(4);

        assertThat(subtask.getPosition()).isEqualTo(4);
    }

    @Test
    void aStoryConsolidatesOnlyWhenThereIsAtLeastOneSubtaskAndAllAreDone() {
        Subtask other = subtask(14, 1);

        assertThat(Subtask.allDone(List.of())).isFalse();

        subtask.changeStatus(SubtaskStatus.DONE);
        assertThat(Subtask.allDone(List.of(subtask, other))).isFalse();

        other.changeStatus(SubtaskStatus.DONE);
        assertThat(Subtask.allDone(List.of(subtask, other))).isTrue();

        other.changeStatus(SubtaskStatus.IN_REVIEW);
        assertThat(Subtask.allDone(List.of(subtask, other))).isFalse();
    }
}
