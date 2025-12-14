package com.oilgas.calculations.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler implementing RFC 9457 Problem Details
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_TYPE_BASE = "https://api.oilgas.com/problems/";

    @ExceptionHandler(CalculationNotFoundException.class)
    public ProblemDetail handleCalculationNotFound(CalculationNotFoundException ex) {
        log.warn("Calculation not found: {}", ex.getCalculationId());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "calculation-not-found"));
        problem.setTitle("Calculation Not Found");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("calculationId", ex.getCalculationId());
        problem.setProperty("suggestion", ex.getSuggestion());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(CalculationLimitExceededException.class)
    public ProblemDetail handleCalculationLimitExceeded(CalculationLimitExceededException ex) {
        log.warn("Calculation limit exceeded: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "calculation-limit-exceeded"));
        problem.setTitle("Calculation Limit Exceeded");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("suggestion", ex.getSuggestion());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ProblemDetail handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage(), ex.getCause());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "service-unavailable"));
        problem.setTitle("Service Unavailable");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("suggestion", ex.getSuggestion());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(CalculationException.class)
    public ProblemDetail handleCalculationException(CalculationException ex) {
        log.error("Calculation error: {} - {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "calculation-error"));
        problem.setTitle("Calculation Error");
        problem.setProperty("errorCode", ex.getErrorCode());
        if (ex.getCalculationId() != null) {
            problem.setProperty("calculationId", ex.getCalculationId());
        }
        if (ex.getSuggestion() != null) {
            problem.setProperty("suggestion", ex.getSuggestion());
        }
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        var errors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint validation failed"
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("errorCode", "CONSTRAINT_VIOLATION");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker open: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service is temporarily unavailable due to circuit breaker"
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "circuit-breaker-open"));
        problem.setTitle("Service Temporarily Unavailable");
        problem.setProperty("errorCode", "CIRCUIT_BREAKER_OPEN");
        problem.setProperty("suggestion", "The calculation service is experiencing issues. Please wait and try again.");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    private record ValidationError(String field, String message, Object rejectedValue) {}
}
