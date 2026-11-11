package com.mentaiko.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.mentaiko.backend.validation.Utf8Password;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @Size(min = 8, max = 72) @Utf8Password String newPassword
) {}
