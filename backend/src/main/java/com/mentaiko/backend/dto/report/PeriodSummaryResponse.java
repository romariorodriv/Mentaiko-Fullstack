package com.mentaiko.backend.dto.report;

public record PeriodSummaryResponse(
        String week,
        long count,
        double averageIntensity,
        String predominantEmotion
) {
}
