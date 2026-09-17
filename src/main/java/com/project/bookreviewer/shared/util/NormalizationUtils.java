package com.project.bookreviewer.shared.util;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // Treat hyphens as word separators so "sci-fi" and "Sci Fi" share a key
        normalized = normalized.replace('-', ' ');
        // Remove punctuation
        normalized = PUNCTUATION.matcher(normalized).replaceAll("");
        // Trim and collapse whitespace
        normalized = WHITESPACE.matcher(normalized.trim()).replaceAll(" ");
        // Lowercase
        return normalized.toLowerCase();
    }

    /**
     * Comparison key for genres (same rules as {@link #normalize(String)}).
     */
    public static String genreKey(String input) {
        return normalize(input);
    }

    /**
     * Display label for a genre: cleaned then Title Case
     */
    public static String toGenreLabel(String input) {
        String key = genreKey(input);
        if (key == null || key.isBlank()) {
            return null;
        }
        StringBuilder label = new StringBuilder();
        // Cuts the string on every space into an array of words
        for (String word : key.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            // If it is not the first word, then add a space after it
            if (!label.isEmpty()) {
                label.append(' ');
            }
            // Uppercase the first letter of the word
            label.append(Character.toUpperCase(word.charAt(0)));
            // Add the rest of the word
            if (word.length() > 1) {
                label.append(word.substring(1));
            } 
        }
        return label.isEmpty() ? null : label.toString();
    }

    /**
     * Expands requested genre filters to all catalog labels that share the same genreKey,
     * plus the canonical display label for each request.
     */
    public static Set<String> expandMatchingGenres(Set<String> requested, Collection<String> catalog) {
        if (requested == null || requested.isEmpty()) {
            return Set.of();
        }
        // Build a list of keys of the selected genres
        Set<String> keys = new LinkedHashSet<>();
        for (String genre : requested) {
            String key = genreKey(genre);
            if (key != null && !key.isBlank()) {
                keys.add(key);
            }
        }
        if (keys.isEmpty()) {
            return Set.of();
        }

        // Pull matching aliases from the catalog (if genre isn't stored normalized)
        Set<String> expanded = new LinkedHashSet<>();
        if (catalog != null) {
            for (String genre : catalog) {
                String key = genreKey(genre);
                if (key != null && !key.isBlank()) {
                    expanded.add(key);
                }
            }
        }
        // Include a clean display form
        for (String genre : requested) {
            String label = toGenreLabel(genre);
            if (label != null) {
                expanded.add(label);
            }
        }

        return expanded;
    }

    /**
     * One display label per genreKey (canonical Title Case).
     */
    public static List<String> dedupeGenreLabels(Collection<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }
        Map<String, String> byKey = new LinkedHashMap<>();
        for (String genre : genres) {
            String key = genreKey(genre);
            if (key == null || key.isBlank()) {
                continue;
            }
            String label = toGenreLabel(genre);
            byKey.putIfAbsent(key, label != null ? label : genre);
        }

        return List.copyOf(byKey.values());
    }

}
