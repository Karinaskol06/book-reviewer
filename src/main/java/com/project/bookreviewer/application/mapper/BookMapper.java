package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.BookDetailResponse;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.domain.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookMapper {
    @Mapping(target = "ratingStats", ignore = true)
    @Mapping(target = "userReadingStatus", ignore = true)
    @Mapping(target = "userHasReviewed", ignore = true)
    @Mapping(target = "recommendationReason", ignore = true)
    @Mapping(target = "coverUrl", source = "coverUrl",
            defaultValue = "src/main/resources/static/images/book_placeholder.png")
    BookResponse toResponse(Book book);

    @Mapping(target = "ratingStats", ignore = true)
    @Mapping(target = "userReadingStatus", ignore = true)
    @Mapping(target = "userHasReviewed", ignore = true)
    @Mapping(target = "coverUrl", source = "coverUrl",
            defaultValue = "src/main/resources/static/images/book_placeholder.png")
    BookDetailResponse toDetailResponse(Book book);
}
