package com.project.bookreviewer.infrastructure.elasticsearch.repository;

import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewSearchRepository extends ElasticsearchRepository<ReviewDocument, String> {
}