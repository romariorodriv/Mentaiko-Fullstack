package com.mentaiko.backend.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record CreateRecommendationRequest(
        @NotNull
        Long checkinId
) {
}
