package com.project.bookreviewer.domain.model;

import com.project.bookreviewer.shared.util.NormalizationUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a book's raw genre tags into the genres that say something about a reader's taste.
 * Tags are matched by {@link NormalizationUtils#genreKey(String)}, so case, hyphens and spacing don't matter.
 */
public final class GenreTaxonomy {

    /** Tags so broad they describe almost every book on a shelf. */
    private static final Set<String> UMBRELLA_KEYS = Set.of("fiction", "nonfiction", "non fiction", "novels");

    /** Combined shelf tags (mostly Goodreads-style) and the genres they stand for. */
    private static final Map<String, List<String>> COMBINED_KEYS = Map.of(
            "science fiction fantasy", List.of("Science Fiction", "Fantasy"),
            "sci fi fantasy", List.of("Science Fiction", "Fantasy"),
            "mystery thriller", List.of("Mystery", "Thriller")
    );

    private GenreTaxonomy() {
    }

    /**
     * @return trimmed display labels, without umbrella tags, with combined tags split, each genre at most once
     */
    public static Set<String> tasteGenres(Collection<String> rawGenres) {
        if (rawGenres == null) {
            return Set.of();
        }
        Map<String, String> labelsByKey = new LinkedHashMap<>();
        for (String raw : rawGenres) {
            String key = NormalizationUtils.genreKey(raw);
            if (key == null || key.isBlank() || UMBRELLA_KEYS.contains(key)) {
                continue;
            }
            for (String label : COMBINED_KEYS.getOrDefault(key, List.of(raw.trim()))) {
                labelsByKey.putIfAbsent(NormalizationUtils.genreKey(label), label);
            }
        }
        return new LinkedHashSet<>(labelsByKey.values());
    }
}
