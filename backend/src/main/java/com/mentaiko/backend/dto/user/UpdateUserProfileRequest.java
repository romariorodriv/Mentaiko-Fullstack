package com.mentaiko.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String name,

        @NotBlank(message = "La universidad es obligatoria")
        @Size(max = 150, message = "La universidad no debe superar 150 caracteres")
        String university,

        @NotBlank(message = "La carrera es obligatoria")
        @Size(max = 150, message = "La carrera no debe superar 150 caracteres")
        String career
) {
}
