package com.project.bookreviewer.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
            "sci-fi, Scifi"
    })
    void toGenreLabel_formatsCanonicalDisplay(String input, String expected) {
        assertThat(NormalizationUtils.toGenreLabel(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "romance, romance",
            "RoManCE, romance",
            "ROMANCE....., romance",
            "Historical Fiction!, historical fiction"
    })
    void genreKey_collapsesCasingAndPunctuation(String input, String expected) {
        assertThat(NormalizationUtils.genreKey(input)).isEqualTo(expected);
    }
}
