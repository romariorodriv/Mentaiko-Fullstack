package com.mentaiko.backend.dto.report;

public record CompareResponse(
        PeriodSummaryResponse weekA,
        PeriodSummaryResponse weekB
) {
}
