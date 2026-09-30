package com.mentaiko.backend.dto.recommendation;

public record RecommendationActivityResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        boolean active,
        String createdAt,
        String updatedAt
) {
}
