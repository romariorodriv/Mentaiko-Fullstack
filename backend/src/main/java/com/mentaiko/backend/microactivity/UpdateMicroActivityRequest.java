package com.mentaiko.backend.dto.microactivity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateMicroActivityRequest(
        @Size(max = 120)
        String title,

        @Size(max = 500)
        String description,

        @Min(1)
        @Max(120)
        Integer durationMinutes,

        Boolean active
) {
}
