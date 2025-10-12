package com.oilgas.calculations.model;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

/**
 * Request for field work calculations
 */
@Builder
public record CalculationRequest(
        @NotEmpty(message = "At least one equation required")
        List<@NotBlank String> equations,

        @NotEmpty(message = "Initial parameters required")
        List<@NotNull Double> initialParameters,

        @NotNull(message = "Calculation options required")
        CalculationOptions options,

        WellConfiguration wellConfig
) {

    /**
     * Solver configuration options
     */
    @Builder
    public record CalculationOptions(
            String solverMethod,
            Integer maxIterations,
            Double tolerance,
            String unitSystem
    ) {
        public CalculationOptions {
            if (solverMethod == null || solverMethod.isBlank()) {
                solverMethod = "hybr";
            }
            if (maxIterations == null) {
                maxIterations = 1000;
            }
            if (tolerance == null) {
                tolerance = 1e-8;
            }
            if (unitSystem == null || unitSystem.isBlank()) {
                unitSystem = "metric";
            }
        }
    }

    /**
     * Well configuration for field operations
     */
    @Builder
    public record WellConfiguration(
            @NotBlank String wellName,
            @NotBlank String fieldName,
            @Positive Double depthMeters,
            @Positive Double diameterInches,
            @NotBlank String fluidType
    ) {
    }
}
