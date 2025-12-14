package com.oilgas.calculations.exception;

/**
 * Exception thrown when concurrent calculation limit is exceeded
 */
public class CalculationLimitExceededException extends CalculationException {
    public CalculationLimitExceededException(int currentCount, int maxAllowed) {
        super(
                String.format("Maximum concurrent calculations exceeded. Current: %d, Max: %d", currentCount, maxAllowed),
                "CALCULATION_LIMIT_EXCEEDED",
                null,
                "Wait for existing calculations to complete or cancel some before starting new ones."
        );
    }
}
