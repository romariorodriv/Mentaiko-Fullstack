package com.mentaiko.backend.dto.checkin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCheckinRequest(

        @NotNull
        Long emotionId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer intensity,

        @NotBlank
        @Size(max = 100)
        String context,

        @Size(max = 500)
        String note

) {
}
