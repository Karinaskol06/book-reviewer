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
    private int sampleSize;
    private int reviewSampleSize;

    @Data
    @Builder
    public static class GenreShare {
        private String name;
        private int weight;
        private int sharePercent;
    }

    @Data
    @Builder
    public static class MoodCount {
        private String name;
        private int count;
    }
}
