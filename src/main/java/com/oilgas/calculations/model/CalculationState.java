package com.oilgas.calculations.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Possible states of a calculation
 */
@Schema(description = "Calculation lifecycle states")
public enum CalculationState {
    @Schema(description = "Calculation has been created and queued")
    PENDING,

    @Schema(description = "Calculation has started processing")
    STARTED,

    @Schema(description = "Calculation is actively running")
    CALCULATING,

    @Schema(description = "Calculation completed successfully")
    COMPLETED,

    @Schema(description = "Calculation failed with an error")
    FAILED,

    @Schema(description = "Calculation was cancelled by user")
    CANCELLED
}
