package com.oilgas.calculations.model;

import lombok.Builder;

/**
 * Response after initiating calculation
 */
@Builder
public record CalculationResponse(
        String calculationId,
        String status,
        String message,
        String wsSubscriptionPath,
        Long estimatedTimeSeconds
) {
    public static CalculationResponse started(String calculationId) {
        return CalculationResponse.builder()
                .calculationId(calculationId)
                .status("STARTED")
                .message("Calculation initiated")
                .wsSubscriptionPath("/topic/calculation/" + calculationId)
                .estimatedTimeSeconds(30L)
                .build();
    }
}