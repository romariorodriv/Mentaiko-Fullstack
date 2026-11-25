package com.mentaiko.backend.dto.focus;

import java.time.LocalDateTime;

public record FocusSessionResponse(
        Long id,
        Long activityId,
        Long recommendationId,
        int durationSeconds,
        LocalDateTime completedAt
) {
}
