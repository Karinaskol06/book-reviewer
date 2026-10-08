package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.TasteProfileResponse;

/**
 * Driving port for computed taste / genre spectrum.
 */
public interface TasteProfileUseCase {
    TasteProfileResponse getTasteProfile(Long userId);
}
