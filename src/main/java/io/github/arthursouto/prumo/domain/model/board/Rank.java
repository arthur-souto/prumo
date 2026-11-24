package io.github.arthursouto.prumo.domain.model.board;

import java.util.ArrayList;
import java.util.List;

public final class Rank {

    public static final int MAX_LENGTH = 40;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final int SPREAD_WIDTH = 3;

    private Rank() {
    }

    public static String between(String lower, String upper) {
        String low = lower == null ? "" : lower;
        String high = upper;
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (true) {
            int lowDigit = index < low.length() ? ALPHABET.indexOf(low.charAt(index)) : 0;
            int highDigit = high != null && index < high.length() ? ALPHABET.indexOf(high.charAt(index)) : BASE;

            if (lowDigit == highDigit) {
                result.append(ALPHABET.charAt(lowDigit));
                index++;
                continue;
            }

            int middle = (lowDigit + highDigit) / 2;

            if (middle > lowDigit) {
                result.append(ALPHABET.charAt(middle));
                return result.toString();
            }

            result.append(ALPHABET.charAt(lowDigit));
            high = null;
            index++;
        }
    }

    public static List<String> spread(int count) {
        long total = (long) Math.pow(BASE, SPREAD_WIDTH);
        long step = total / (count + 1);
        List<String> ranks = new ArrayList<>(count);

        for (int position = 1; position <= count; position++) {
            String rank = encode(step * position);

            if (rank.endsWith("0")) {
                rank = encode(step * position + 1);
            }

            ranks.add(rank);
        }

        return ranks;
    }

    public static boolean isExhausted(String rank) {
        return rank.length() > MAX_LENGTH;
    }

    private static String encode(long value) {
        char[] digits = new char[SPREAD_WIDTH];
        long remaining = value;

        for (int position = SPREAD_WIDTH - 1; position >= 0; position--) {
            digits[position] = ALPHABET.charAt((int) (remaining % BASE));
            remaining /= BASE;
        }

        return new String(digits);
    }
}
