package com.mentaiko.backend.dto.checkin;

import java.time.LocalDateTime;

public record CheckinResponse(
        Long id,
        CheckinEmotionResponse emotion,
        int intensity,
        String context,
        String note,
        LocalDateTime createdAt
) {
}
