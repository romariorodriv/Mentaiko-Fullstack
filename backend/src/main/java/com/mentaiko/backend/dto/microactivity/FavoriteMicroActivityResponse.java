package com.mentaiko.backend.dto.microactivity;

import java.time.LocalDateTime;

public record FavoriteMicroActivityResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        boolean active,
        String activityType,
        LocalDateTime savedAt
) {
}
