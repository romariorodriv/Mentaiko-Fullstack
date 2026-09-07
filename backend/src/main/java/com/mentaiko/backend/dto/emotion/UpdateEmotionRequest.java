package com.mentaiko.backend.dto.emotion;

import jakarta.validation.constraints.Size;

public record UpdateEmotionRequest(

        @Size(min = 2, max = 80)
        String name,

        Boolean active

) {
}
