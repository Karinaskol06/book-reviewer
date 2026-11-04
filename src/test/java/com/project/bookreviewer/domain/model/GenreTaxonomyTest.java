package com.project.bookreviewer.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenreTaxonomyTest {

    @Test
    void regularTags_areKeptTrimmed() {
        assertThat(GenreTaxonomy.tasteGenres(List.of(" Dark Academia ", "Gothic")))
                .containsExactly("Dark Academia", "Gothic");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Fiction", " fiction ", "Nonfiction", "Non-Fiction", "Novels"})
    void umbrellaTags_areDropped(String umbrella) {
        assertThat(GenreTaxonomy.tasteGenres(List.of(umbrella, "Gothic")))
                .containsExactly("Gothic");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Science Fiction Fantasy", "Sci-Fi Fantasy", "science fiction fantasy"})
    void scienceFictionFantasy_splitsIntoBothGenres(String combined) {
        assertThat(GenreTaxonomy.tasteGenres(List.of(combined)))
                .containsExactly("Science Fiction", "Fantasy");
    }

    @Test
    void mysteryThriller_splitsIntoBothGenres() {
        assertThat(GenreTaxonomy.tasteGenres(List.of("Mystery Thriller")))
                .containsExactly("Mystery", "Thriller");
    }

    @Test
    void partAlreadyTaggedSeparately_appearsOnce() {
        assertThat(GenreTaxonomy.tasteGenres(List.of("Fantasy", "Science Fiction Fantasy")))
                .containsExactly("Fantasy", "Science Fiction");
    }

    @Test
    void nullAndBlankTags_areIgnored() {
        assertThat(GenreTaxonomy.tasteGenres(Arrays.asList(null, "  ", "Horror")))
                .containsExactly("Horror");
        assertThat(GenreTaxonomy.tasteGenres(null)).isEmpty();
    }
}
