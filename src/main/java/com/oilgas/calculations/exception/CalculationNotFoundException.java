package com.oilgas.calculations.exception;

/**
 * Exception thrown when a calculation is not found
 */
public class CalculationNotFoundException extends CalculationException {
    public CalculationNotFoundException(String calculationId) {
        super(
                "Calculation not found: " + calculationId,
                "CALCULATION_NOT_FOUND",
                calculationId,
                "Verify the calculation ID is correct. Calculations are removed after completion."
        );
    }
}
