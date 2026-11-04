package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class TasteProfileResponse {
    @Builder.Default
    private List<GenreShare> topGenres = new ArrayList<>();
    @Builder.Default
    private List<MoodCount> topMoods = new ArrayList<>();
    private String dominantPacing;
    private Double averageRating;
    /** Books on the shelf (or highly rated reviews, as a fallback) that contributed at least one genre. */
    private int sampleSize;
    private int reviewSampleSize;
    /** Distinct genres beyond {@code topGenres}. */
    private int otherGenreCount;
    /** Every genre, same order as {@code topGenres} (which is its first entries). */
    @Builder.Default
    private List<GenreShare> allGenres = new ArrayList<>();

    @Data
    @Builder
    public static class GenreShare {
        private String name;
        /** Taste weight: reading status plus a bonus for high ratings, summed over the genre's books. */
        private int weight;
        /** How many sample books carry this genre (a book can carry several genres). */
        private int bookCount;
    }

    @Data
    @Builder
    public static class MoodCount {
        private String name;
        private int count;
    }
}
