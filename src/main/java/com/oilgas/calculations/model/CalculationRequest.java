package com.oilgas.calculations.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

/**
 * Request for field work calculations
 */
@Builder
@Schema(description = "Request to initiate a new calculation")
public record CalculationRequest(
        @NotEmpty(message = "At least one equation required")
        @Schema(
                description = "List of equations to solve",
                example = "[\"x + y = 10\", \"x - y = 2\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<@NotBlank String> equations,

        @NotEmpty(message = "Initial parameters required")
        @Schema(
                description = "Initial parameter values for the solver",
                example = "[1.0, 1.0]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<@NotNull Double> initialParameters,

        @NotNull(message = "Calculation options required")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        CalculationOptions options,

        @Schema(description = "Well configuration for field operations (optional)")
        WellConfiguration wellConfig
) {

    /**
     * Solver configuration options
     */
    @Builder
    @Schema(description = "Solver configuration options")
    public record CalculationOptions(
            @Schema(
                    description = "Numerical solver method to use",
                    example = "hybr",
                    allowableValues = {"hybr", "lm", "broyden1"},
                    defaultValue = "hybr"
            )
            String solverMethod,

            @Schema(
                    description = "Maximum number of solver iterations",
                    example = "1000",
                    defaultValue = "1000",
                    minimum = "1",
                    maximum = "100000"
            )
            @Min(value = 1, message = "maxIterations must be at least 1")
            @Max(value = 100000, message = "maxIterations cannot exceed 100000")
            Integer maxIterations,

            @Schema(
                    description = "Convergence tolerance",
                    example = "1e-8",
                    defaultValue = "1e-8",
                    minimum = "1e-15",
                    maximum = "1"
            )
            @Positive(message = "tolerance must be positive")
            Double tolerance,

            @Schema(
                    description = "Unit system for results",
                    example = "metric",
                    allowableValues = {"metric", "imperial"},
                    defaultValue = "metric"
            )
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
    @Schema(description = "Well configuration for oil & gas field operations")
    public record WellConfiguration(
            @NotBlank
            @Schema(
                    description = "Name of the well",
                    example = "Well-A1",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String wellName,

            @NotBlank
            @Schema(
                    description = "Name of the oil/gas field",
                    example = "North Sea Field",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String fieldName,

            @Positive
            @Schema(
                    description = "Well depth in meters",
                    example = "3500.0",
                    minimum = "0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Double depthMeters,

            @Positive
            @Schema(
                    description = "Well bore diameter in inches",
                    example = "8.5",
                    minimum = "0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Double diameterInches,

            @NotBlank
            @Schema(
                    description = "Type of fluid for the operation",
                    example = "drilling_mud",
                    allowableValues = {"drilling_mud", "cement", "completion_fluid", "spacer_fluid", "displacement_fluid"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String fluidType
    ) {
    }
}
