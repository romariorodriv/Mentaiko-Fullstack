package com.mentaiko.backend.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecommendationFeedbackRequest(
        @NotNull(message = "La valoracion es obligatoria")
        @Min(value = 1, message = "La valoracion debe estar entre 1 y 5")
        @Max(value = 5, message = "La valoracion debe estar entre 1 y 5")
        Integer helpfulnessRating,

        @Size(max = 300, message = "El comentario no puede superar 300 caracteres")
        String feedbackComment
) {
}
