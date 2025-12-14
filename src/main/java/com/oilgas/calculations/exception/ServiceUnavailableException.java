package com.oilgas.calculations.exception;

/**
 * Exception thrown when backend service is unavailable
 */
public class ServiceUnavailableException extends CalculationException {
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(
                "Service unavailable: " + serviceName,
                "SERVICE_UNAVAILABLE",
                null,
                "The calculation service is temporarily unavailable. Please try again later."
        );
        initCause(cause);
    }
}
