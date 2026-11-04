package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.TasteProfileResponse;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.GenreTaxonomy;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.inbound.TasteProfileUseCase;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TasteProfileService implements TasteProfileUseCase {
    static final int HIGH_RATING_THRESHOLD = 4;
    static final int TOP_GENRES = 5;
    static final int TOP_MOODS = 3;
    private static final int REVIEW_PAGE_SIZE = 500;

    private static final int WEIGHT_READ = 3;
    private static final int WEIGHT_READING = 2;
    private static final int WEIGHT_WANT = 1;
    private static final int WEIGHT_HIGH_RATING_BONUS = 2;
    private static final int WEIGHT_REVIEW_FALLBACK = 3;

    /** Heaviest taste signal first; ties broken alphabetically. */
    private static final Comparator<GenreTally> HEAVIEST_FIRST =
            Comparator.comparingInt(GenreTally::weight).reversed()
                    .thenComparing(GenreTally::name);

    /** Genres on the most books first; ties broken by taste weight. */
    private static final Comparator<GenreTally> MOST_BOOKS_FIRST =
            Comparator.comparingInt(GenreTally::bookCount).reversed()
                    .thenComparing(HEAVIEST_FIRST);

    private final UserBookStatusRepositoryPort statusRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final BookRepositoryPort bookRepository;

    @Transactional(readOnly = true)
    public TasteProfileResponse getTasteProfile(Long userId) {
        List<Review> reviews = findReviews(userId);
        List<GenreTally> genres = tallyGenres(userId, reviews).stream()
                .sorted(MOST_BOOKS_FIRST)
                .toList();
        long sampleSize = genres.stream()
                .flatMap(genre -> genre.bookIds().stream())
                .distinct()
                .count();

        List<TasteProfileResponse.GenreShare> allGenres = genres.stream().map(GenreTally::toShare).toList();

        return TasteProfileResponse.builder()
                .topGenres(allGenres.stream().limit(TOP_GENRES).toList())
                .allGenres(allGenres)
                .otherGenreCount(Math.max(0, allGenres.size() - TOP_GENRES))
                .topMoods(toMoodCounts(reviews, TOP_MOODS))
                .dominantPacing(dominantPacing(reviews))
                .averageRating(averageRating(reviews))
                .sampleSize((int) sampleSize)
                .reviewSampleSize(reviews.size())
                .build();
    }

    /**
     * Genre labels used by recommendations: same shelf-first / high-rated-review fallback rules,
     * ranked by taste weight so finished and highly rated books lead.
     */
    @Transactional(readOnly = true)
    public List<String> resolveTasteGenreLabels(Long userId, int limit) {
        return tallyGenres(userId, findReviews(userId)).stream()
                .sorted(HEAVIEST_FIRST)
                .limit(Math.max(limit, 0))
                .map(GenreTally::name)
                .toList();
    }

    private List<Review> findReviews(Long userId) {
        return reviewRepository.findByUserId(userId, PageRequest.of(0, REVIEW_PAGE_SIZE)).getContent();
    }

    /**
     * Shelf books (minus abandoned ones) weighted by status and rating; if the shelf yields no genres,
     * falls back to highly rated reviews.
     */
    private Collection<GenreTally> tallyGenres(Long userId, List<Review> reviews) {
        Map<Long, Integer> ratingByBookId = reviews.stream()
                .filter(r -> r.getBookId() != null && r.getRating() != null)
                .collect(Collectors.toMap(Review::getBookId, Review::getRating, Math::max));
        Map<String, GenreTally> tallies = new HashMap<>();

        for (UserBookStatus status : statusRepository.findByUserId(userId)) {
            if (status.getStatus() == ReadingStatus.ABANDONED || status.getBookId() == null) {
                continue;
            }
            int weight = statusWeight(status.getStatus());
            Integer rating = ratingByBookId.get(status.getBookId());
            if (rating != null && rating >= HIGH_RATING_THRESHOLD) {
                weight += WEIGHT_HIGH_RATING_BONUS;
            }
            final int appliedWeight = weight;
            bookRepository.findById(status.getBookId())
                    .ifPresent(book -> addGenres(book, appliedWeight, tallies));
        }

        if (tallies.isEmpty()) {
            for (Review review : reviews) {
                if (review.getRating() == null || review.getRating() < HIGH_RATING_THRESHOLD || review.getBookId() == null) {
                    continue;
                }
                bookRepository.findById(review.getBookId())
                        .ifPresent(book -> addGenres(book, WEIGHT_REVIEW_FALLBACK, tallies));
            }
        }
        return tallies.values();
    }

    private int statusWeight(ReadingStatus status) {
        return switch (status) {
            case READ -> WEIGHT_READ;
            case READING -> WEIGHT_READING;
            case WANT_TO_READ -> WEIGHT_WANT;
            case ABANDONED -> 0;
        };
    }

    private void addGenres(Book book, int weight, Map<String, GenreTally> tallies) {
        if (weight <= 0) {
            return;
        }
        for (String genre : GenreTaxonomy.tasteGenres(book.getGenres())) {
            tallies.computeIfAbsent(genre, GenreTally::new).add(book.getId(), weight);
        }
    }

    private List<TasteProfileResponse.MoodCount> toMoodCounts(List<Review> reviews, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        for (Review review : reviews) {
            if (review.getMood() == null) {
                continue;
            }
            for (String mood : review.getMood()) {
                if (mood == null || mood.isBlank()) {
                    continue;
                }
                counts.merge(mood.trim(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> TasteProfileResponse.MoodCount.builder()
                        .name(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private String dominantPacing(List<Review> reviews) {
        Map<Pacing, Long> counts = reviews.stream()
                .map(Review::getPacing)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<Pacing, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(e -> e.getKey().name()))
                .map(e -> e.getKey().name())
                .orElse(null);
    }

    private Double averageRating(List<Review> reviews) {
        var ratings = reviews.stream()
                .map(Review::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        if (ratings.isEmpty()) {
            return null;
        }
        return Math.round(ratings.getAsDouble() * 10.0) / 10.0;
    }

    /** Running total for one genre: which books carry it and their combined taste weight. */
    private static final class GenreTally {
        private final String name;
        private final Set<Long> bookIds = new HashSet<>();
        private int weight;

        private GenreTally(String name) {
            this.name = name;
        }

        private void add(Long bookId, int bookWeight) {
            if (bookIds.add(bookId)) {
                weight += bookWeight;
            }
        }

        private String name() {
            return name;
        }

        private Set<Long> bookIds() {
            return bookIds;
        }

        private int bookCount() {
            return bookIds.size();
        }

        private int weight() {
            return weight;
        }

        private TasteProfileResponse.GenreShare toShare() {
            return TasteProfileResponse.GenreShare.builder()
                    .name(name)
                    .weight(weight)
                    .bookCount(bookCount())
                    .build();
        }
    }
}
