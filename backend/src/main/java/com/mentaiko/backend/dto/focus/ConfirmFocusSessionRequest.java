package com.mentaiko.backend.dto.focus;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmFocusSessionRequest(
        @NotNull UUID requestId,
        @NotNull @Positive Long activityId,
        @Positive Long recommendationId,
        @NotNull @Min(60) @Max(7200) Integer durationSeconds
) {
}
