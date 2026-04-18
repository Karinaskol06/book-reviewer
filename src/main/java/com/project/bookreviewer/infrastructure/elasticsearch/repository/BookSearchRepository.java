package com.project.bookreviewer.infrastructure.elasticsearch.repository;

import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, Long> {
}