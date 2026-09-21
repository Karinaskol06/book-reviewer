package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.TasteProfileResponse;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class TasteProfileService {
    static final int HIGH_RATING_THRESHOLD = 4;
    static final int TOP_GENRES = 4;
    static final int TOP_MOODS = 3;
    private static final int REVIEW_PAGE_SIZE = 500;

    private static final int WEIGHT_READ = 3;
    private static final int WEIGHT_READING = 2;
    private static final int WEIGHT_WANT = 1;
    private static final int WEIGHT_HIGH_RATING_BONUS = 2;
    private static final int WEIGHT_REVIEW_FALLBACK = 3;

    private final UserBookStatusRepositoryPort statusRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final BookRepositoryPort bookRepository;

    @Transactional(readOnly = true)
    public TasteProfileResponse getTasteProfile(Long userId) {
        List<UserBookStatus> statuses = statusRepository.findByUserId(userId);
        List<Review> reviews = reviewRepository.findByUserId(userId, PageRequest.of(0, REVIEW_PAGE_SIZE)).getContent();
        Map<Long, Integer> ratingByBookId = reviews.stream()
                .filter(r -> r.getBookId() != null && r.getRating() != null)
                .collect(Collectors.toMap(Review::getBookId, Review::getRating, Math::max));

        Map<String, Integer> genreWeights = new HashMap<>();
        Set<Long> sampleBookIds = new HashSet<>();

        for (UserBookStatus status : statuses) {
            if (status.getStatus() == ReadingStatus.ABANDONED || status.getBookId() == null) {
                continue;
            }
            int weight = statusWeight(status.getStatus());
            Integer rating = ratingByBookId.get(status.getBookId());
            if (rating != null && rating >= HIGH_RATING_THRESHOLD) {
                weight += WEIGHT_HIGH_RATING_BONUS;
            }
            final int appliedWeight = weight;
            bookRepository.findById(status.getBookId()).ifPresent(book -> {
                addGenres(book, genreWeights, appliedWeight);
                sampleBookIds.add(book.getId());
            });
        }

        if (genreWeights.isEmpty()) {
            for (Review review : reviews) {
                if (review.getRating() == null || review.getRating() < HIGH_RATING_THRESHOLD || review.getBookId() == null) {
                    continue;
                }
                bookRepository.findById(review.getBookId()).ifPresent(book -> {
                    addGenres(book, genreWeights, WEIGHT_REVIEW_FALLBACK);
                    sampleBookIds.add(book.getId());
                });
            }
        }

        List<TasteProfileResponse.GenreShare> topGenres = toGenreShares(genreWeights, TOP_GENRES);
        List<TasteProfileResponse.MoodCount> topMoods = toMoodCounts(reviews, TOP_MOODS);
        String dominantPacing = dominantPacing(reviews);
        Double averageRating = averageRating(reviews);

        return TasteProfileResponse.builder()
                .topGenres(topGenres)
                .topMoods(topMoods)
                .dominantPacing(dominantPacing)
                .averageRating(averageRating)
                .sampleSize(sampleBookIds.size())
                .reviewSampleSize(reviews.size())
                .build();
    }

    /**
     * Genre labels used by recommendations — same shelf-first / high-rated-review fallback rules.
     */
    @Transactional(readOnly = true)
    public List<String> resolveTasteGenreLabels(Long userId, int limit) {
        TasteProfileResponse profile = getTasteProfile(userId);
        return profile.getTopGenres().stream()
                .limit(Math.max(limit, 0))
                .map(TasteProfileResponse.GenreShare::getName)
                .toList();
    }

    private int statusWeight(ReadingStatus status) {
        return switch (status) {
            case READ -> WEIGHT_READ;
            case READING -> WEIGHT_READING;
            case WANT_TO_READ -> WEIGHT_WANT;
            case ABANDONED -> 0;
        };
    }

    private void addGenres(Book book, Map<String, Integer> weights, int weight) {
        if (book.getGenres() == null || weight <= 0) {
            return;
        }
        for (String genre : book.getGenres()) {
            if (genre == null || genre.isBlank()) {
                continue;
            }
            weights.merge(genre.trim(), weight, Integer::sum);
        }
    }

    private List<TasteProfileResponse.GenreShare> toGenreShares(Map<String, Integer> weights, int limit) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        return weights.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> TasteProfileResponse.GenreShare.builder()
                        .name(entry.getKey())
                        .weight(entry.getValue())
                        .sharePercent(total == 0 ? 0 : (int) Math.round(entry.getValue() * 100.0 / total))
                        .build())
                .toList();
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
}
