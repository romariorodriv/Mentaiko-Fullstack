package com.mentaiko.backend.dto.report;

public record DistributionItemResponse(
        String label,
        long count,
        double percentage
) {
}
