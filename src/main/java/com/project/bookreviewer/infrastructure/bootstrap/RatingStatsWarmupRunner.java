package com.project.bookreviewer.infrastructure.bootstrap;

import com.project.bookreviewer.application.service.BookService;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.repository.JpaBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RatingStatsWarmupRunner implements ApplicationRunner {
    private final BookService bookService;
    private final JpaBookRepository jpaBookRepository;

    @Override
    public void run(ApplicationArguments args) {
        var ids = jpaBookRepository.findAll().stream().map(BookEntity::getId).toList();
        if (ids.isEmpty()) {
            return;
        }
        log.info("Refreshing cached rating stats for {} books", ids.size());
        ids.forEach(bookService::updateBookRatingStats);
    }
}
