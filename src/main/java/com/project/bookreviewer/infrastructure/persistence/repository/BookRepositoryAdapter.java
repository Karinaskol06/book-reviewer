package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import com.project.bookreviewer.shared.util.NormalizationUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookRepositoryAdapter implements BookRepositoryPort {
    private final JpaBookRepository jpaBookRepository;
    private final JpaReviewRepository jpaReviewRepository;

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
    public Optional<Book> findByNormalizedTitleAndNormalizedAuthor(String normalizedTitle, String normalizedAuthor) {
        return jpaBookRepository.findByNormalizedTitleAndNormalizedAuthor(normalizedTitle, normalizedAuthor)
                .map(this::mapToDomain);
    }

    @Override
    public List<Book> search(String query, int page, int size) {
        return jpaBookRepository.search(query, PageRequest.of(page, size))
                .stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public long countSearch(String query) {
        return jpaBookRepository.countSearch(query);
    }

    @Override
    public Page<Book> filterBooks(BookFilterCriteria criteria, Pageable pageable) {
        List<Long> dominantPacingBookIds = null;
        if (criteria.getPacing() != null && !criteria.getPacing().isEmpty()) {
            dominantPacingBookIds = jpaReviewRepository.findBookIdsByDominantPacingIn(
                    criteria.getPacing().stream().map(Enum::name).collect(Collectors.toList()));
            if (dominantPacingBookIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        Set<String> expandedGenres = null;
        if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
            expandedGenres = NormalizationUtils.expandMatchingGenres(
                    criteria.getGenres(),
                    jpaBookRepository.findAllGenres()
            );
            if (expandedGenres.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        return jpaBookRepository.findAll(buildFilterSpec(criteria, dominantPacingBookIds, expandedGenres), pageable)
                .map(this::mapToDomain);
    }

    private Specification<BookEntity> buildFilterSpec(
            BookFilterCriteria criteria,
            List<Long> dominantPacingBookIds,
            Set<String> expandedGenres
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (expandedGenres != null) {
                Join<BookEntity, String> genresJoin = root.join("genres");
                predicates.add(genresJoin.in(expandedGenres));
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
            if (dominantPacingBookIds != null) {
                predicates.add(root.get("id").in(dominantPacingBookIds));
            }
            if (Boolean.TRUE.equals(criteria.getContentSafe())) {
                Subquery<Long> warnedReview = query.subquery(Long.class);
                Root<ReviewEntity> reviewRoot = warnedReview.from(ReviewEntity.class);
                warnedReview.select(reviewRoot.get("id"))
                        .where(
                                cb.equal(reviewRoot.get("bookId"), root.get("id")),
                                cb.gt(cb.size(reviewRoot.get("contentWarnings")), 0)
                        );
                predicates.add(cb.not(cb.exists(warnedReview)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<String> findAllGenres() {
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
    public boolean existsByNormalizedTitleAndNormalizedAuthor(String normalizedTitle, String normalizedAuthor) {
        return jpaBookRepository.existsByNormalizedTitleAndNormalizedAuthor(normalizedTitle, normalizedAuthor);
    }

    @Override
    public long count() {
        return jpaBookRepository.count();
    }


    private BookEntity mapToEntity(Book book) {
        String normalizedAuthor = book.getNormalizedAuthor() != null
                ? book.getNormalizedAuthor()
                : NormalizationUtils.normalize(book.getAuthor());
        return BookEntity.builder()
                .id(book.getId())
                .title(book.getTitle())
                .normalizedTitle(book.getNormalizedTitle())
                .author(book.getAuthor())
                .normalizedAuthor(normalizedAuthor)
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
        String normalizedAuthor = entity.getNormalizedAuthor() != null
                ? entity.getNormalizedAuthor()
                : NormalizationUtils.normalize(entity.getAuthor());
        return Book.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .normalizedTitle(entity.getNormalizedTitle())
                .author(entity.getAuthor())
                .normalizedAuthor(normalizedAuthor)
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
