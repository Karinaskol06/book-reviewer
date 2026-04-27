package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookRepositoryAdapter implements BookRepositoryPort {
    private final JpaBookRepository jpaBookRepository;

    @Override
    public Book save(Book book) {
        BookEntity entity = mapToEntity(book);
        BookEntity saved = jpaBookRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Book> findById(Long id) {
        return jpaBookRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<Book> findByNormalizedTitleAndAuthor(String normalizedTitle, String author) {
        return jpaBookRepository.findByNormalizedTitleAndAuthor(normalizedTitle, author)
                .map(this::mapToDomain);
    }

    @Override
    public List<Book> search(String query, int page, int size) {
        return jpaBookRepository.search(query, PageRequest.of(page, size))
                .stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public Page<Book> filterBooks(BookFilterCriteria criteria, Pageable pageable) {
        Specification<BookEntity> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
                Join<BookEntity, String> genresJoin = root.join("genres");
                predicates.add(genresJoin.in(criteria.getGenres()));
            }
            if (criteria.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"),
                        criteria.getMinRating().doubleValue()));
            }
            if (criteria.getYearFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publicationYear"), criteria.getYearFrom()));
            }
            if (criteria.getYearTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publicationYear"), criteria.getYearTo()));
            }
            if (criteria.getSearchQuery() != null && !criteria.getSearchQuery().isBlank()) {
                String likePattern = "%" + criteria.getSearchQuery().toLowerCase() + "%";
                Join<BookEntity, String> searchGenresJoin = root.join("genres", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("author")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern),
                        cb.like(cb.lower(searchGenresJoin), likePattern)
                ));
            }
            // Pacing and contentSafe would require joins to Review; skip for now or implement later

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BookEntity> entityPage = jpaBookRepository.findAll(spec, pageable);
        return entityPage.map(this::mapToDomain);
    }

    @Override
    public List<String> findAllGenres() {
        // Could be a separate table or aggregated fom books
        return jpaBookRepository.findAllGenres();
    }

    @Override
    public List<Book> findAll(int page, int size) {
        return jpaBookRepository.findAll(PageRequest.of(page, size))
                .stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<Book> findByGenre(String genre, int page, int size) {
        return jpaBookRepository.findByGenre(genre, PageRequest.of(page, size))
                .stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<Book> findTrending(int limit) {
        return jpaBookRepository.findTrending(limit)
                .stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Book> findFeatured() {
        return jpaBookRepository.findFeatured().map(this::mapToDomain);
    }

    @Override
    public boolean existsByNormalizedTitleAndAuthor(String normalizedTitle, String author) {
        return jpaBookRepository.existsByNormalizedTitleAndAuthor(normalizedTitle, author);
    }

    @Override
    public long count() {
        return jpaBookRepository.count();
    }


    private BookEntity mapToEntity(Book book) {
        return BookEntity.builder()
                .id(book.getId())
                .title(book.getTitle())
                .normalizedTitle(book.getNormalizedTitle())
                .author(book.getAuthor())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .publicationYear(book.getPublicationYear())
                .genres(book.getGenres())
                .createdAt(book.getCreatedAt())
                .averageRating(book.getAverageRating())
                .ratingCount(book.getRatingCount())
                .totalReviews(book.getTotalReviews())
                .build();
    }

    private Book mapToDomain(BookEntity entity) {
        return Book.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .normalizedTitle(entity.getNormalizedTitle())
                .author(entity.getAuthor())
                .description(entity.getDescription())
                .coverUrl(entity.getCoverUrl())
                .publicationYear(entity.getPublicationYear())
                .genres(entity.getGenres())
                .createdAt(entity.getCreatedAt())
                .averageRating(entity.getAverageRating())
                .ratingCount(entity.getRatingCount())
                .totalReviews(entity.getTotalReviews())
                .build();
    }
}
