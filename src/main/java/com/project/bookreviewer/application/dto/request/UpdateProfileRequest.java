package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(max = 120)
    private String displayName;

    @Size(max = 1500)
    private String aboutMe;

    @Size(max = 5)
    private List<@Size(max = 500) String> socialLinks = new ArrayList<>();
}
