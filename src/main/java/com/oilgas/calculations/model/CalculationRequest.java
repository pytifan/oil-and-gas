package com.oilgas.calculations.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * Request for well completion calculations.
 * {@code wellParams} must be provided to run the physics-based simulation.
 */
@Builder
@Schema(description = "Request to initiate a new well completion calculation")
public record CalculationRequest(

        @NotNull(message = "Calculation options required")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        CalculationOptions options,

        @Schema(description = "Well metadata (optional)")
        WellConfiguration wellConfig,

        @Schema(description = "Well completion parameters — required to run the physics-based simulation")
        WellParameters wellParams
) {

    /**
     * Calculation options
     */
    @Builder
    @Schema(description = "Calculation options")
    public record CalculationOptions(
            @Schema(
                    description = "Unit system for results",
                    example = "metric",
                    allowableValues = {"metric", "imperial"},
                    defaultValue = "metric"
            )
            String unitSystem
    ) {
        public CalculationOptions {
            if (unitSystem == null || unitSystem.isBlank()) {
                unitSystem = "metric";
            }
        }
    }

    /**
     * Physics-based well completion parameters
     */
    @Builder
    @Schema(description = "Well geometry and fluid properties for completion simulation")
    public record WellParameters(
            @Schema(description = "Tubing length [m]", example = "4000.0", defaultValue = "4000.0")
            Double tubingLengthM,

            @Schema(description = "Tubing outer diameter [mm]", example = "89.0", defaultValue = "89.0")
            Double tubingOdMm,

            @Schema(description = "Tubing wall thickness [mm]", example = "6.5", defaultValue = "6.5")
            Double tubingWallMm,

            @Schema(description = "Casing outer diameter [mm]", example = "168.0", defaultValue = "168.0")
            Double casingOdMm,

            @Schema(description = "Casing wall thickness [mm]", example = "10.0", defaultValue = "10.0")
            Double casingWallMm,

            @Schema(description = "Completion fluid density [kg/m³]", example = "1020.0", defaultValue = "1020.0")
            Double fluidDensityKgM3,

            @Schema(description = "Gravitational acceleration [m/s²]", example = "9.81", defaultValue = "9.81")
            Double gravityMpS2,

            @Schema(description = "Initial fluid level depth [m]", example = "0.0", defaultValue = "0.0")
            Double initialWaterLevelM,

            @Schema(description = "Surface injection pressure [Pa]", example = "100000", defaultValue = "100000")
            Double surfacePressurePa,

            @Schema(description = "Maximum wellhead back-pressure [Pa]", example = "2000000", defaultValue = "2000000")
            Double maxWellheadPressurePa,

            @Schema(description = "Minimum wellhead back-pressure [Pa]", example = "1000000", defaultValue = "1000000")
            Double minWellheadPressurePa
    ) {
        public WellParameters {
            if (tubingLengthM     == null) tubingLengthM     = 4000.0;
            if (tubingOdMm        == null) tubingOdMm        = 89.0;
            if (tubingWallMm      == null) tubingWallMm      = 6.5;
            if (casingOdMm        == null) casingOdMm        = 168.0;
            if (casingWallMm      == null) casingWallMm      = 10.0;
            if (fluidDensityKgM3  == null) fluidDensityKgM3  = 1020.0;
            if (gravityMpS2       == null) gravityMpS2       = 9.81;
            if (initialWaterLevelM == null) initialWaterLevelM = 0.0;
            if (surfacePressurePa  == null) surfacePressurePa  = 1e5;
            if (maxWellheadPressurePa == null) maxWellheadPressurePa = 200e5;
            if (minWellheadPressurePa == null) minWellheadPressurePa = 100e5;
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
