package com.mentaiko.backend.dto.recommendation;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WellnessChatRequest(
        @NotBlank @Size(max = 500) String message,
        @NotNull @Size(max = 6) List<@Valid Message> history
) {
    public record Message(
            @NotBlank String role,
            @NotBlank @Size(max = 500) String content
    ) {}
}
