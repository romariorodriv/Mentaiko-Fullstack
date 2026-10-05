package com.mentaiko.backend.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mentaiko.backend.enums.Role;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        LocalDate birthDate,
        String university,
        String career,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
}
