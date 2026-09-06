package com.mentaiko.backend.dto.auth;

public record LoginResponse(
        String token,
        UserResponse user
) {
}
