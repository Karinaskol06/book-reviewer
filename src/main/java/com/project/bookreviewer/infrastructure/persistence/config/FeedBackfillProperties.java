package com.project.bookreviewer.infrastructure.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.feed.backfill")
public class FeedBackfillProperties {
    private int days = 7;
    private int limit = 20;
}