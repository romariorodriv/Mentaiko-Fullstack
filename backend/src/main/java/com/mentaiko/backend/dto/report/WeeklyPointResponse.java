package com.mentaiko.backend.dto.report;

public record WeeklyPointResponse(
        String label,
        long count,
        double averageIntensity
) {
}
