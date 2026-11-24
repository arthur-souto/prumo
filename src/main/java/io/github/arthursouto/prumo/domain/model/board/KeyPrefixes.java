package io.github.arthursouto.prumo.domain.model.board;

import java.text.Normalizer;
import java.util.Locale;

public final class KeyPrefixes {

    private static final int DERIVED_LENGTH = 3;
    private static final String FALLBACK = "WS";

    private KeyPrefixes() {
    }

    public static String derive(String workspaceName) {
        String letters = Normalizer.normalize(workspaceName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z]", "");

        if (letters.length() < 2) {
            return FALLBACK;
        }

        return letters.substring(0, Math.min(DERIVED_LENGTH, letters.length()));
    }
}
