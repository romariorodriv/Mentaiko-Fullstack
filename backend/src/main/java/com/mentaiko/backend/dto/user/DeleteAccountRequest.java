package com.mentaiko.backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "Confirma tu contrasena") String password
) {}
