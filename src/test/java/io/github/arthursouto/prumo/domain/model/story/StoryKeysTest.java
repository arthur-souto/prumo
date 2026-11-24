package io.github.arthursouto.prumo.domain.model.story;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryKeysTest {

    @Test
    void findsAKeyInABranchName() {
        assertThat(StoryKeys.extract("feature/PRU-12-reset-password")).containsExactly("PRU-12");
    }

    @Test
    void findsAKeyInATitleCaseInsensitively() {
        assertThat(StoryKeys.extract("fix pru-7: crash on login")).containsExactly("PRU-7");
    }

    @Test
    void findsKeysAcrossBranchAndTitleWithoutDuplicates() {
        assertThat(StoryKeys.extract("feature/PRU-1-x", "PRU-1 and PRU-2")).containsExactly("PRU-1", "PRU-2");
    }

    @Test
    void supportsPrefixesWithDigits() {
        assertThat(StoryKeys.extract("PRU2-30 done")).containsExactly("PRU2-30");
    }

    @Test
    void ignoresTextThatCannotBeAKey() {
        assertThat(StoryKeys.extract("XPRU-1abc", "1-2", "-5", "PRU-")).isEmpty();
    }

    @Test
    void anythingShapedLikeAKeyIsReturnedAndTheCallerKeepsOnlyRealStories() {
        assertThat(StoryKeys.extract("release-2024")).containsExactly("RELEASE-2024");
    }

    @Test
    void ignoresNulls() {
        assertThat(StoryKeys.extract(null, "PRU-3")).containsExactly("PRU-3");
        assertThat(StoryKeys.extract((String) null)).isEmpty();
    }
}
