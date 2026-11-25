package com.mentaiko.backend.dto.recommendation;

import java.time.LocalDateTime;

import com.mentaiko.backend.enums.RecommendationDiscardReason;

public record RecommendationResponse(
        Long id,
        Long checkinId,
        RecommendationActivityResponse activity,
        String reason,
        boolean fallbackUsed,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        Integer helpfulnessRating,
        String feedbackComment,
        Integer preIntensity,
        Integer postIntensity,
        LocalDateTime discardedAt,
        RecommendationDiscardReason discardReason
) {
}
