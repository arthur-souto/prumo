package io.github.arthursouto.prumo.domain.model.board;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyPrefixesTest {

    @Test
    void takesTheFirstThreeLettersUppercased() {
        assertThat(KeyPrefixes.derive("Prumo")).isEqualTo("PRU");
    }

    @Test
    void ignoresAccentsDigitsAndSpaces() {
        assertThat(KeyPrefixes.derive("Édição 2 Alpha")).isEqualTo("EDI");
    }

    @Test
    void keepsShortNames() {
        assertThat(KeyPrefixes.derive("Go")).isEqualTo("GO");
    }

    @Test
    void fallsBackWhenThereAreNotEnoughLetters() {
        assertThat(KeyPrefixes.derive("42")).isEqualTo("WS");
        assertThat(KeyPrefixes.derive("A1")).isEqualTo("WS");
    }
}
