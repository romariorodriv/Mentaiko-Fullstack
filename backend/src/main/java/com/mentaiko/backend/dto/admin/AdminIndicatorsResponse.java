package com.mentaiko.backend.dto.admin;

import java.util.List;

import com.mentaiko.backend.dto.report.DistributionItemResponse;

public record AdminIndicatorsResponse(
        long totalUsers,
        long activeUsers,
        long totalCheckins,
        List<DistributionItemResponse> emotionDistribution,
        long recommendationsGenerated,
        long recommendationsCompleted,
        double completionRate,
        String mostFrequentEmotion
) {
}
