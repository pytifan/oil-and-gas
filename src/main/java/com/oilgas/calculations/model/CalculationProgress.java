package com.oilgas.calculations.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * WebSocket/SSE messages for real-time calculation updates.
 * Uses sealed interface with JSON type discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CalculationProgress.Progress.class, name = "progress"),
        @JsonSubTypes.Type(value = CalculationProgress.Result.class, name = "result"),
        @JsonSubTypes.Type(value = CalculationProgress.Error.class, name = "error")
})
@Schema(description = "Real-time calculation update message")
public sealed interface CalculationProgress {
    String calculationId();
    String type();

    /**
     * Progress update during calculation
     */
    @Builder
    @Schema(description = "Progress update sent during calculation execution")
    record Progress(
            @Schema(description = "Unique calculation identifier")
            String calculationId,

            @Schema(description = "Message type discriminator", example = "progress")
            String type,

            @Schema(description = "Completion percentage (0-100)", example = "45")
            int percentage,

            @Schema(description = "Current calculation phase", example = "SOLVING_EQUATIONS")
            String phase,

            @Schema(description = "Current iteration number", example = "150")
            int iteration,

            @Schema(description = "Current convergence metric value", example = "0.00001")
            double convergenceMetric,

            @Schema(description = "Human-readable progress message")
            String message,

            @Schema(description = "Current fluid type being processed")
            String currentFluidType
    ) implements CalculationProgress {

        public Progress(String calculationId, int percentage, String phase,
                        int iteration, double convergenceMetric, String message) {
            this(calculationId, "progress", percentage, phase, iteration,
                    convergenceMetric, message, null);
        }
    }

    /**
     * Final calculation result
     */
    @Builder
    @Schema(description = "Final calculation result with computed volumes")
    record Result(
            @Schema(description = "Unique calculation identifier")
            String calculationId,

            @Schema(description = "Message type discriminator", example = "result")
            String type,

            @Schema(description = "List of computed volume requirements")
            List<CalculationResult.VolumeRequirement> volumes,

            @Schema(description = "Calculation metadata including timing and convergence info")
            CalculationResult.CalculationMetadata metadata
    ) implements CalculationProgress {

        public Result(String calculationId,
                      List<CalculationResult.VolumeRequirement> volumes,
                      CalculationResult.CalculationMetadata metadata) {
            this(calculationId, "result", volumes, metadata);
        }
    }

    /**
     * Calculation error
     */
    @Builder
    @Schema(description = "Error message when calculation fails")
    record Error(
            @Schema(description = "Unique calculation identifier")
            String calculationId,

            @Schema(description = "Message type discriminator", example = "error")
            String type,

            @Schema(description = "Machine-readable error code", example = "CONVERGENCE_FAILED")
            String errorCode,

            @Schema(description = "Human-readable error message")
            String errorMessage,

            @Schema(description = "Suggested action to resolve the error")
            String suggestion
    ) implements CalculationProgress {

        public Error(String calculationId, String errorCode, String errorMessage) {
            this(calculationId, "error", errorCode, errorMessage, null);
        }
    }
}
