package com.project.bookreviewer.shared.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class NormalizationUtils {

    private static final Pattern PUNCTUATION = Pattern.compile("\\p{Punct}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NormalizationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Normalizes a string for comparison purposes.
     * - Trims leading/trailing whitespace
     * - Converts to lowercase
     * - Removes all non-alphanumeric characters (optional, adjust as needed)
     *
     * @param input the raw string
     * @return normalized string, or null if input is null
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        // Remove diacritics (é -> e)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        // Remove punctuation
        normalized = PUNCTUATION.matcher(normalized).replaceAll("");
        // Trim and collapse whitespace
        normalized = WHITESPACE.matcher(normalized.trim()).replaceAll(" ");
        // Lowercase
        return normalized.toLowerCase();
    }

}
