package io.github.arthursouto.prumo.domain.model.story;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStoryTest {

    private UserStory story;
    private ChecklistItem criterion;

    @BeforeEach
    void setUp() {
        criterion = ChecklistItem.create("works", false);
        story = UserStory.create(
                UUID.randomUUID(),
                12,
                "PRU-12",
                "Reset password",
                "desc",
                StoryPriority.HIGH,
                5,
                UUID.randomUUID(),
                "V",
                List.of("api"),
                List.of(criterion)
        );
    }

    @Test
    void newStoriesCarryTheirKeyAndStartUnlinked() {
        assertThat(story.getKey()).isEqualTo("PRU-12");
        assertThat(story.getNumber()).isEqualTo(12);
        assertThat(story.getRepositoryId()).isNull();
        assertThat(story.getPullRequests()).isEmpty();
        assertThat(story.getCreatedAt()).isEqualTo(story.getUpdatedAt());
    }

    @Test
    void updateReplacesTheEditableFieldsAndTouchesTheTimestamp() throws InterruptedException {
        Thread.sleep(2);

        story.update("New", null, StoryPriority.LOW, null, List.of("x"), List.of());

        assertThat(story.getTitle()).isEqualTo("New");
        assertThat(story.getDescription()).isNull();
        assertThat(story.getPriority()).isEqualTo(StoryPriority.LOW);
        assertThat(story.getStoryPoints()).isNull();
        assertThat(story.getLabels()).containsExactly("x");
        assertThat(story.getAcceptanceCriteria()).isEmpty();
        assertThat(story.getUpdatedAt()).isAfter(story.getCreatedAt());
        assertThat(story.getKey()).isEqualTo("PRU-12");
    }

    @Test
    void moveChangesColumnAndRank() {
        UUID column = UUID.randomUUID();

        story.moveTo(column, "k");

        assertThat(story.getColumnId()).isEqualTo(column);
        assertThat(story.getRank()).isEqualTo("k");
    }

    @Test
    void tickingACriterionWorks() {
        assertThat(story.setChecklistItemDone(criterion.id(), true)).isTrue();

        assertThat(story.getAcceptanceCriteria()).extracting(ChecklistItem::done).containsExactly(true);
    }

    @Test
    void tickingAnUnknownItemReportsFalse() {
        assertThat(story.setChecklistItemDone(UUID.randomUUID(), true)).isFalse();
    }

    @Test
    void linkingAndUnlinkingARepositoryTouchesTheStory() {
        UUID repository = UUID.randomUUID();

        story.linkRepository(repository);
        assertThat(story.getRepositoryId()).isEqualTo(repository);

        story.unlinkRepository();
        assertThat(story.getRepositoryId()).isNull();
    }

    @Test
    void collectionsAreImmutableViews() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> story.getLabels().add("hack"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
