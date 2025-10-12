package com.oilgas.calculations.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.oilgas.calculations.grpc.CalculationsProto.CalculationResult.volumeRequirement;
import com.oilgas.calculations.grpc.CalculationsProto.CalculationResult.CalculationMetadata;
import lombok.Builder;

import java.util.List;

/**
 * WebSocket messages for real-time calculation updates
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CalculationProgress.Progress.class, name = "progress"),
        @JsonSubTypes.Type(value = CalculationProgress.Result.class, name = "result"),
        @JsonSubTypes.Type(value = CalculationProgress.Error.class, name = "error")
})
public sealed interface CalculationProgress {
    String calculationId();
    String type();

    /**
     * Progress update during calculation
     */
    @Builder
    record Progress(
            String calculationId,
            String type,
            int percentage,
            String phase,
            int iteration,
            double convergenceMetric,
            String message,
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
    record Result(
            String calculationId,
            String type,
            List<VolumeRequirement> volumes,
            CalculationMetadata metadata
    ) implements CalculationProgress {

        public Result(String calculationId,
                      List<VolumeRequirement> volumes,
                      CalculationMetadata metadata) {
            this(calculationId, "result", volumes, metadata);
        }
    }

    /**
     * Calculation error
     */
    @Builder
    record Error(
            String calculationId,
            String type,
            String errorCode,
            String errorMessage,
            String suggestion
    ) implements CalculationProgress {

        public Error(String calculationId, String errorCode, String errorMessage) {
            this(calculationId, "error", errorCode, errorMessage, null);
        }
    }
}