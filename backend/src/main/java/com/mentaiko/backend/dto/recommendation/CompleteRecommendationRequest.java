package com.mentaiko.backend.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cuerpo opcional de POST /api/recommendations/{id}/complete.
 * postIntensity es la intensidad que la persona percibe despues de la microactividad.
 */
public record CompleteRecommendationRequest(
        @Min(value = 1, message = "La intensidad posterior debe estar entre 1 y 5")
        @Max(value = 5, message = "La intensidad posterior debe estar entre 1 y 5")
        Integer postIntensity
) {
}
