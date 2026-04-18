package com.project.bookreviewer.infrastructure.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Set;

@Document(indexName = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String author;

    @Field(type = FieldType.Keyword)
    private Set<String> genres;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer publicationYear;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String coverUrl;

    // Aggregated pacing from reviews (most common)
    @Field(type = FieldType.Keyword)
    private String dominantPacing;

    // Common moods across reviews
    @Field(type = FieldType.Keyword)
    private Set<String> commonMoods;

    @Field(type = FieldType.Boolean)
    private Boolean hasContentWarnings;
}