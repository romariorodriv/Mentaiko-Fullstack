package com.mentaiko.backend.entity;

import com.mentaiko.backend.enums.Role;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        return role == null ? Role.USER.name() : role.name();
    }

    @Override
    public Role convertToEntityAttribute(String value) {
        if (value == null || value.isBlank() || "STUDENT".equalsIgnoreCase(value)) {
            return Role.USER;
        }
        return Role.valueOf(value.toUpperCase());
    }
}
