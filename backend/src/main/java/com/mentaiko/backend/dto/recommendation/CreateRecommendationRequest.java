package com.mentaiko.backend.dto.recommendation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record CreateRecommendationRequest(
        @NotNull
        Long checkinId,
        @Min(1) @Max(120) Integer maxDurationMinutes
) {
}
