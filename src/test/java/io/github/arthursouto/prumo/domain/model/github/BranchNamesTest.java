package io.github.arthursouto.prumo.domain.model.github;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BranchNamesTest {

    @Test
    void buildsTheKeyAndASlugOfTheTitle() {
        assertThat(BranchNames.suggest("PRU-12", "Reset the Password!")).isEqualTo("feature/PRU-12-reset-the-password");
    }

    @Test
    void removesAccents() {
        assertThat(BranchNames.suggest("PRU-1", "Autenticação de usuário")).isEqualTo("feature/PRU-1-autenticacao-de-usuario");
    }

    @Test
    void usesOnlyTheKeyWhenTheTitleHasNoUsableCharacters() {
        assertThat(BranchNames.suggest("PRU-2", "???")).isEqualTo("feature/PRU-2");
    }

    @Test
    void truncatesLongTitlesWithoutTrailingDashes() {
        String name = BranchNames.suggest("PRU-3", "a ".repeat(60));

        assertThat(name.length()).isLessThanOrEqualTo("feature/PRU-3-".length() + 50);
        assertThat(name).doesNotEndWith("-");
    }
}
