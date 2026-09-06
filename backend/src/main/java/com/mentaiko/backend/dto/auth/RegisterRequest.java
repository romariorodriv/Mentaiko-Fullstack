package com.mentaiko.backend.dto.auth;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        @Size(max = 150)
        String university,

        @NotBlank
        @Size(max = 150)
        String career

) {
}