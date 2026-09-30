package com.mentaiko.backend.dto.microactivity;

import java.time.LocalDateTime;

public record MicroActivityResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
