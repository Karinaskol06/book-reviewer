package com.project.bookreviewer.application.dto.request;

import com.project.bookreviewer.domain.model.Pacing;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class CreateReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @NotBlank
    private String verdict;

    private String detailedReview;

    private Pacing pacing;

    private Set<String> mood;

    @NotBlank
    private String whoIsItFor;

    @NotBlank
    private String whoIsItNotFor;

    private Set<String> contentWarnings;

    private String spoilerContent;

    private Boolean hasSpoiler = false;

    private Set<String> tags;
}