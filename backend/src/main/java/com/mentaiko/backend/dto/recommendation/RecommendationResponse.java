package com.mentaiko.backend.dto.recommendation;

import java.time.LocalDateTime;

public record RecommendationResponse(
        Long id,
        Long checkinId,
        RecommendationActivityResponse activity,
        String reason,
        boolean fallbackUsed,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
