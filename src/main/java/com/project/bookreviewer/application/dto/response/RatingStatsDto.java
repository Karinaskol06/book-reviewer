package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class RatingStatsDto {
    private Double average;
    private Integer total;
    private Map<Integer, Integer> distribution; // e.g., 5: 82, 4: 12, ...
}
