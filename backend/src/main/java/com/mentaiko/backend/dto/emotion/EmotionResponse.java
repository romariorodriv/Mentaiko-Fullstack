package com.mentaiko.backend.dto.emotion;

import java.time.LocalDateTime;

public record EmotionResponse(
        Long id,
        String name,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
