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

@Document(indexName = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDocument {
    @Id
    private String id; // composite: userId_bookId

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Long)
    private Long bookId;

    @Field(type = FieldType.Integer)
    private Integer rating;

    @Field(type = FieldType.Keyword)
    private String pacing;

    @Field(type = FieldType.Keyword)
    private Set<String> mood;

    @Field(type = FieldType.Keyword)
    private Set<String> tags;

    @Field(type = FieldType.Text)
    private String verdict;
}