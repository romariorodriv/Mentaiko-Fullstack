package com.mentaiko.backend.dto.report;

import java.util.List;

public record DistributionResponse(
        List<DistributionItemResponse> emotions,
        List<DistributionItemResponse> contexts
) {
}
