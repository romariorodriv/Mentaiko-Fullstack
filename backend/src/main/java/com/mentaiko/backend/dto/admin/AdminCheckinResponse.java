package com.mentaiko.backend.dto.admin;

import java.time.LocalDateTime;

public record AdminCheckinResponse(
        Long id,
        AdminCheckinUserResponse user,
        AdminCheckinEmotionResponse emotion,
        int intensity,
        String context,
        LocalDateTime createdAt
) {
}
