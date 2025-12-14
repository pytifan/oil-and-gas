package com.oilgas.calculations.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Response after initiating calculation
 */
@Builder
@Schema(description = "Response returned when a calculation is created")
public record CalculationResponse(
        @Schema(description = "Unique calculation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String calculationId,

        @Schema(description = "Current calculation status", example = "STARTED")
        String status,

        @Schema(description = "Human-readable status message", example = "Calculation initiated successfully")
        String message,

        @Schema(description = "WebSocket subscription path for real-time updates (legacy)",
                example = "/topic/calculation/550e8400-e29b-41d4-a716-446655440000")
        String wsSubscriptionPath,

        @Schema(description = "SSE stream URL for real-time progress updates (recommended)",
                example = "/api/v1/calculations/550e8400-e29b-41d4-a716-446655440000/progress")
        String sseStreamUrl,

        @Schema(description = "Estimated calculation time in seconds", example = "30")
        Long estimatedTimeSeconds
) {
    public static CalculationResponse started(String calculationId) {
        return CalculationResponse.builder()
                .calculationId(calculationId)
                .status("STARTED")
                .message("Calculation initiated successfully")
                .wsSubscriptionPath("/topic/calculation/" + calculationId)
                .sseStreamUrl("/api/v1/calculations/" + calculationId + "/progress")
                .estimatedTimeSeconds(30L)
                .build();
    }
}
