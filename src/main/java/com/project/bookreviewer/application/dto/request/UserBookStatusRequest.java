package com.project.bookreviewer.application.dto.request;

import com.project.bookreviewer.domain.model.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserBookStatusRequest {
    @NotNull
    private ReadingStatus status;
}
