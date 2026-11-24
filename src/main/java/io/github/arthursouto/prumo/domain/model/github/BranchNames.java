package io.github.arthursouto.prumo.domain.model.github;

import java.text.Normalizer;
import java.util.Locale;

public final class BranchNames {

    private static final int MAX_SLUG_LENGTH = 50;

    private BranchNames() {
    }

    public static String suggest(String storyKey, String title) {
        String slug = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }

        return slug.isEmpty() ? "feature/" + storyKey : "feature/" + storyKey + "-" + slug;
    }
}
