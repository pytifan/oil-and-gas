package com.oilgas.calculations.model.api;

import com.oilgas.calculations.model.CalculationState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

/**
 * Response for calculation status queries
 */
@Builder
@Schema(description = "Calculation status information")
public record CalculationStatusResponse(
        @Schema(description = "Unique calculation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String calculationId,

        @Schema(description = "Whether the calculation is currently active")
        boolean active,

        @Schema(description = "Current calculation state")
        CalculationState status,

        @Schema(description = "Current progress percentage (0-100)", example = "45")
        Integer progressPercentage,

        @Schema(description = "Current phase of calculation", example = "SOLVING_EQUATIONS")
        String currentPhase,

        @Schema(description = "When the calculation was started")
        Instant startedAt,

        @Schema(description = "When the calculation completed (if finished)")
        Instant completedAt,

        @Schema(description = "SSE stream URL for real-time progress updates")
        String progressStreamUrl
) {
    public static CalculationStatusResponse from(String calculationId, CalculationState state,
                                                  long startTime, Integer progress, String phase) {
        return CalculationStatusResponse.builder()
                .calculationId(calculationId)
                .active(state == CalculationState.STARTED || state == CalculationState.CALCULATING)
                .status(state)
                .progressPercentage(progress)
                .currentPhase(phase)
                .startedAt(Instant.ofEpochMilli(startTime))
                .completedAt(state == CalculationState.COMPLETED || state == CalculationState.FAILED
                        ? Instant.now() : null)
                .progressStreamUrl("/api/v1/calculations/" + calculationId + "/progress")
                .build();
    }
}
