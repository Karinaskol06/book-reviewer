package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowStats {
    private long followers;
    private long following;
}