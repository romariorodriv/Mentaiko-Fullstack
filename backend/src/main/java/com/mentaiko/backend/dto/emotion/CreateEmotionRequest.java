package com.mentaiko.backend.dto.emotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmotionRequest(

        @NotBlank
        @Size(min = 2, max = 80)
        String name

) {
}
