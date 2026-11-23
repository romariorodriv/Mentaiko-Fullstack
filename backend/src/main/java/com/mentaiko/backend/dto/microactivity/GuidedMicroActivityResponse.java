package com.mentaiko.backend.dto.microactivity;

import java.util.List;

public record GuidedMicroActivityResponse(
        Long id,
        String title,
        Integer durationMinutes,
        List<GuideStep> steps
) {
    public record GuideStep(String title, String instruction) {
    }
}
