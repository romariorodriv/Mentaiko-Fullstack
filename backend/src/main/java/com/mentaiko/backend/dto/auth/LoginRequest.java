package com.mentaiko.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.mentaiko.backend.validation.Utf8Password;


public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Utf8Password
        String password

) {
}
