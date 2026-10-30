package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotNull
    private Long userId;
}
