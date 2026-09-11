package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.BookDetailResponse;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class BookMapper {

    @Autowired
    protected ObjectStoragePort objectStoragePort;

    @Mapping(target = "ratingStats", ignore = true)
    @Mapping(target = "userReadingStatus", ignore = true)
    @Mapping(target = "userHasReviewed", ignore = true)
    @Mapping(target = "recommendationReason", ignore = true)
    @Mapping(target = "coverUrl", source = "coverUrl", qualifiedByName = "toPublicCoverUrl")
    public abstract BookResponse toResponse(Book book);

    @Mapping(target = "ratingStats", ignore = true)
    @Mapping(target = "userReadingStatus", ignore = true)
    @Mapping(target = "userHasReviewed", ignore = true)
    @Mapping(target = "coverUrl", source = "coverUrl", qualifiedByName = "toPublicCoverUrl")
    public abstract BookDetailResponse toDetailResponse(Book book);

    @Named("toPublicCoverUrl")
    protected String toPublicCoverUrl(String storedReference) {
        if (storedReference == null || storedReference.isBlank()) {
            return null;
        }
        String value = storedReference.trim();
        // Legacy inline covers are not served; clients use the placeholder fallback.
        if (value.startsWith("data:")) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("/uploads-book-reviewer/")) {
            return value;
        }
        return objectStoragePort.toPublicUrl(value);
    }
}
