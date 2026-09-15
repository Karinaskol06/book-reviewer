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
    })
    void normalize_authorVariantsCollapseToSameKey(String input, String expected) {
        assertThat(NormalizationUtils.normalize(input)).isEqualTo(expected);
    }

    @Test
    void normalize_titleStripsPunctuationAndLowercases() {
        assertThat(NormalizationUtils.normalize("The Hobbit!")).isEqualTo("the hobbit");
    }
}
