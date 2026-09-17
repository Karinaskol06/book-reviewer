package com.project.bookreviewer.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "J.R.R. Tolkien, jrr tolkien",
            "jrr. tolkien, jrr tolkien",
            "j.r.r. TOLKIEN, jrr tolkien",
            "Frank Herbert, frank herbert"
    }) // Each line is one run 
    void normalize_authorVariantsCollapseToSameKey(String input, String expected) {
        assertThat(NormalizationUtils.normalize(input)).isEqualTo(expected);
    }

    @Test
    void normalize_titleStripsPunctuationAndLowercases() {
        assertThat(NormalizationUtils.normalize("The Hobbit!")).isEqualTo("the hobbit");
    }

    @ParameterizedTest
    @CsvSource({
            "romance, Romance",
            "RoManCE, Romance",
            "ROMANCE....., Romance",
            "  historical fiction , Historical Fiction",
            "sci-fi, Sci Fi"
    })
    void toGenreLabel_formatsCanonicalDisplay(String input, String expected) {
        assertThat(NormalizationUtils.toGenreLabel(input)).isEqualTo(expected);
    }

    // Run this test once per row in the table below
    @ParameterizedTest
    // Each string is input-expected for one run
    @CsvSource({
            "romance, romance",
            "RoManCE, romance",
            "ROMANCE....., romance",
            "Historical Fiction!, historical fiction",
            "sci-fi, sci fi",
            "Sci Fi, sci fi",
            "SCI-FI, sci fi"
    })
    void genreKey_collapsesCasingAndPunctuation(String input, String expected) {
        assertThat(NormalizationUtils.genreKey(input)).isEqualTo(expected);
    }

    @Test
    void expandMatchingGenres_includesCatalogAliasesForSameKey() {
        // Find every label that shares the same key as "Sci-Fi"
        Set<String> expanded = NormalizationUtils.expandMatchingGenres(
                Set.of("Sci-Fi"),
                List.of("Sci Fi", "Romance", "SCI-FI")
        );

        // Both must be included
        assertThat(expanded).contains("Sci Fi", "SCI-FI");
        assertThat(expanded).doesNotContain("Romance");
        // All returned strings must normalize to sci fi
        assertThat(expanded).allMatch(g -> "sci fi".equals(NormalizationUtils.genreKey(g)));
    }

    @Test
    void dedupeGenreLabels_keepsOneCanonicalLabelPerKey() {
        List<String> deduped = NormalizationUtils.dedupeGenreLabels(
                List.of("Sci Fi", "SCI-FI", "Romance", "romance")
        );

        assertThat(deduped).containsExactlyInAnyOrder("Sci Fi", "Romance");
    }
}
