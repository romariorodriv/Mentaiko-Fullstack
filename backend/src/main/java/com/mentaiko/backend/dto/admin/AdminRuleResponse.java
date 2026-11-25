package com.mentaiko.backend.dto.admin;

import java.time.LocalDateTime;

public record AdminRuleResponse(
        Long id,
        Long emotionId,
        String emotionName,
        boolean emotionActive,
        Long activityId,
        String activityTitle,
        boolean activityActive,
        int priority,
        boolean active,
        LocalDateTime createdAt
) {
}
