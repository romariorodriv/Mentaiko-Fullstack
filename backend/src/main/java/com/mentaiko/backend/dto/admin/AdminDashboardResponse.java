package com.mentaiko.backend.dto.admin;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long totalAdmins,
        long totalCheckins,
        long totalEmotions,
        long totalMicroActivities,
        long totalRecommendations,
        long completedRecommendations
) {
}
