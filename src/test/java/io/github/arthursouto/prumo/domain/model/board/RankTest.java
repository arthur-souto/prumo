package io.github.arthursouto.prumo.domain.model.board;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankTest {

    @Test
    void firstRankInAnEmptyColumnIsAssigned() {
        assertThat(Rank.between(null, null)).isNotBlank();
    }

    @Test
    void rankAfterTheLastSortsAfterIt() {
        String first = Rank.between(null, null);
        String second = Rank.between(first, null);

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void rankBeforeTheFirstSortsBeforeIt() {
        String first = Rank.between(null, null);
        String before = Rank.between(null, first);

        assertThat(before).isLessThan(first);
        assertThat(before).isNotEmpty();
    }

    @Test
    void rankBetweenTwoNeighboursSortsBetweenThem() {
        String low = Rank.between(null, null);
        String high = Rank.between(low, null);
        String middle = Rank.between(low, high);

        assertThat(middle).isGreaterThan(low).isLessThan(high);
    }

    @Test
    void insertingAlwaysAtTheTopKeepsTheOrderStrict() {
        List<String> ranks = new ArrayList<>();
        ranks.add(Rank.between(null, null));

        for (int step = 0; step < 30; step++) {
            ranks.add(0, Rank.between(null, ranks.get(0)));
        }

        for (int index = 1; index < ranks.size(); index++) {
            assertThat(ranks.get(index - 1)).isLessThan(ranks.get(index));
        }
    }

    @Test
    void insertingRepeatedlyInTheSameGapKeepsTheOrderStrict() {
        String low = Rank.between(null, null);
        String high = Rank.between(low, null);
        List<String> inserted = new ArrayList<>();

        for (int step = 0; step < 40; step++) {
            high = Rank.between(low, high);
            inserted.add(high);
        }

        assertThat(inserted).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(inserted).allSatisfy(rank -> assertThat(rank).isGreaterThan(low));
    }

    @Test
    void ranksNeverEndWithTheLowestDigitSoABoundaryAlwaysExists() {
        String rank = null;

        for (int step = 0; step < 200; step++) {
            rank = Rank.between(null, rank);
            assertThat(rank).doesNotEndWith("0");
        }
    }

    @Test
    void longRanksAreReportedAsExhaustedSoTheColumnCanBeRebalanced() {
        String rank = Rank.between(null, null);

        while (!Rank.isExhausted(rank)) {
            rank = Rank.between(null, rank);
        }

        assertThat(rank.length()).isGreaterThan(Rank.MAX_LENGTH);
    }

    @Test
    void spreadReturnsStrictlyIncreasingRanksWithRoomBetweenThem() {
        List<String> ranks = Rank.spread(50);

        assertThat(ranks).hasSize(50).doesNotHaveDuplicates().isSorted();
        assertThat(ranks).allSatisfy(rank -> assertThat(rank).doesNotEndWith("0"));
        assertThat(Rank.between(ranks.get(10), ranks.get(11))).isGreaterThan(ranks.get(10)).isLessThan(ranks.get(11));
    }
}
