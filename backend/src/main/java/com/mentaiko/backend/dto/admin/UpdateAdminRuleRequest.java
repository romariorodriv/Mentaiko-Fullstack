package com.mentaiko.backend.dto.admin;

import jakarta.validation.constraints.Min;

public record UpdateAdminRuleRequest(

        @Min(value = 1, message = "La prioridad debe ser un numero mayor o igual a 1")
        Integer priority,

        Boolean active

) {
}
