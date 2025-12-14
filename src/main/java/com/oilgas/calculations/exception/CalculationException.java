package com.oilgas.calculations.exception;

import lombok.Getter;

/**
 * Base exception for calculation-related errors
 */
@Getter
public class CalculationException extends RuntimeException {
    private final String errorCode;
    private final String calculationId;
    private final String suggestion;

    public CalculationException(String message, String errorCode, String calculationId, String suggestion) {
        super(message);
        this.errorCode = errorCode;
        this.calculationId = calculationId;
        this.suggestion = suggestion;
    }

    public CalculationException(String message, String errorCode) {
        this(message, errorCode, null, null);
    }

    public CalculationException(String message, String errorCode, String calculationId) {
        this(message, errorCode, calculationId, null);
    }
}
