package com.mentaiko.backend.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateAdminRuleRequest(

        @NotNull(message = "Selecciona una emocion")
        Long emotionId,

        @NotNull(message = "Selecciona una microactividad")
        Long activityId,

        @NotNull(message = "La prioridad es obligatoria")
        @Min(value = 1, message = "La prioridad debe ser un numero mayor o igual a 1")
        Integer priority,

        Boolean active

) {
}
