package io.github.arthursouto.prumo.domain.model.story;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StoryKeys {

    private static final Pattern KEY = Pattern.compile("(?<![A-Za-z0-9])([A-Za-z][A-Za-z0-9]{1,9})-(\\d+)(?![A-Za-z0-9])");

    private StoryKeys() {
    }

    public static Set<String> extract(String... texts) {
        Set<String> keys = new LinkedHashSet<>();

        for (String text : texts) {
            if (text == null) {
                continue;
            }

            Matcher matcher = KEY.matcher(text);

            while (matcher.find()) {
                keys.add(matcher.group(1).toUpperCase(Locale.ROOT) + "-" + matcher.group(2));
            }
        }

        return keys;
    }
}
