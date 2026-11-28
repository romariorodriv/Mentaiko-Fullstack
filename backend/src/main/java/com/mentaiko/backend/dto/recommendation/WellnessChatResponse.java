package com.mentaiko.backend.dto.recommendation;

public record WellnessChatResponse(String reply, SuggestedActivity activity) {
    public record SuggestedActivity(Long id, String title, String description, Integer durationMinutes) {}
}
