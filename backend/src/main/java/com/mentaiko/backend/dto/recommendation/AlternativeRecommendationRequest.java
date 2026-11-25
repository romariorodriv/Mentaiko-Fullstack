package com.mentaiko.backend.dto.recommendation;

import com.mentaiko.backend.enums.RecommendationDiscardReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AlternativeRecommendationRequest(
        RecommendationDiscardReason reason,
        @Min(1) @Max(120) Integer availableMinutes
) {
}
