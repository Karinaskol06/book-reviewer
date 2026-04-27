package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAboutMeRequest {
    @Size(max = 1500)
    private String aboutMe;
}
