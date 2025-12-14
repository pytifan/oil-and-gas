package com.oilgas.calculations.service;

import com.oilgas.calculations.config.GatewayConfig;
import com.oilgas.calculations.exception.CalculationLimitExceededException;
import com.oilgas.calculations.exception.CalculationNotFoundException;
import com.oilgas.calculations.model.CalculationProgress;
import com.oilgas.calculations.model.CalculationRequest;
import com.oilgas.calculations.model.CalculationState;
import com.oilgas.calculations.model.api.CalculationStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main service for managing calculations.
 * Uses Java 25 Virtual Threads for efficient concurrent processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationService {

    private final PythonCalculatorClient pythonClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final GatewayConfig gatewayConfig;

    // Java 25 Virtual Threads
    private final ExecutorService virtualThreadExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    // Active calculations tracking
    private final ConcurrentHashMap<String, CalculationStatus> activeCalculations =
            new ConcurrentHashMap<>();

    // SSE sinks for reactive streaming
    private final ConcurrentHashMap<String, Sinks.Many<CalculationProgress>> progressSinks =
            new ConcurrentHashMap<>();

    /**
     * Start a new calculation
     *
     * @param request the calculation request
     * @return the unique calculation ID
     * @throws CalculationLimitExceededException if concurrent limit is exceeded
     */
    public String startCalculation(CalculationRequest request) {
        // Check concurrent calculation limit
        int activeCount = getActiveCalculationCount();
        int maxAllowed = gatewayConfig.getMaxConcurrentCalculations();
        if (activeCount >= maxAllowed) {
            throw new CalculationLimitExceededException(activeCount, maxAllowed);
        }

        String calculationId = UUID.randomUUID().toString();

        log.info("Starting calculation: {} for well: {}",
                calculationId,
                request.wellConfig() != null ? request.wellConfig().wellName() : "N/A");

        // Initialize calculation status
        activeCalculations.put(calculationId, new CalculationStatus(
                calculationId,
                System.currentTimeMillis(),
                CalculationState.STARTED,
                0,
                "INITIALIZING"
        ));

        // Create SSE sink for this calculation
        Sinks.Many<CalculationProgress> sink = Sinks.many().multicast().onBackpressureBuffer();
        progressSinks.put(calculationId, sink);

        // Execute calculation in virtual thread
        CompletableFuture.runAsync(() -> {
            try {
                pythonClient.calculate(
                        calculationId,
                        request,
                        progress -> handleProgressUpdate(calculationId, progress)
                ).join();

                updateCalculationState(calculationId, CalculationState.COMPLETED, 100, "COMPLETED");
                completeSink(calculationId);

            } catch (Exception e) {
                log.error("Calculation failed: {}", calculationId, e);

                CalculationProgress.Error error = CalculationProgress.Error.builder()
                        .calculationId(calculationId)
                        .type("error")
                        .errorCode("CALCULATION_FAILED")
                        .errorMessage(e.getMessage())
                        .suggestion("Check input parameters and try again")
                        .build();

                handleProgressUpdate(calculationId, error);
                updateCalculationState(calculationId, CalculationState.FAILED, null, "FAILED");
                completeSinkWithError(calculationId, e);
            }
        }, virtualThreadExecutor);

        return calculationId;
    }

    /**
     * Get calculation status
     *
     * @param calculationId the calculation ID
     * @return the calculation status
     * @throws CalculationNotFoundException if calculation doesn't exist
     */
    public CalculationStatusResponse getCalculationStatus(String calculationId) {
        CalculationStatus status = activeCalculations.get(calculationId);
        if (status == null) {
            throw new CalculationNotFoundException(calculationId);
        }

        return CalculationStatusResponse.from(
                calculationId,
                status.state(),
                status.startTime(),
                status.progressPercentage(),
                status.currentPhase()
        );
    }

    /**
     * Get progress stream for SSE
     *
     * @param calculationId the calculation ID
     * @return Flux of progress updates
     * @throws CalculationNotFoundException if calculation doesn't exist
     */
    public Flux<CalculationProgress> getProgressStream(String calculationId) {
        if (!activeCalculations.containsKey(calculationId)) {
            throw new CalculationNotFoundException(calculationId);
        }

        Sinks.Many<CalculationProgress> sink = progressSinks.get(calculationId);
        if (sink == null) {
            // Calculation already completed, return empty flux
            return Flux.empty();
        }

        return sink.asFlux()
                .timeout(Duration.ofSeconds(gatewayConfig.getCalculationTimeoutSeconds()))
                .doOnCancel(() -> log.debug("SSE subscription cancelled for: {}", calculationId))
                .doOnTerminate(() -> log.debug("SSE stream terminated for: {}", calculationId));
    }

    /**
     * Cancel a running calculation
     *
     * @param calculationId the calculation ID
     * @throws CalculationNotFoundException if calculation doesn't exist
     */
    public void cancelCalculation(String calculationId) {
        CalculationStatus status = activeCalculations.get(calculationId);
        if (status == null) {
            throw new CalculationNotFoundException(calculationId);
        }

        if (status.state() == CalculationState.COMPLETED || status.state() == CalculationState.FAILED) {
            log.warn("Cannot cancel calculation {} - already in state: {}", calculationId, status.state());
            return;
        }

        log.info("Cancelling calculation: {}", calculationId);
        updateCalculationState(calculationId, CalculationState.CANCELLED, null, "CANCELLED");

        // Send cancellation message
        CalculationProgress.Error cancellation = CalculationProgress.Error.builder()
                .calculationId(calculationId)
                .type("error")
                .errorCode("CALCULATION_CANCELLED")
                .errorMessage("Calculation was cancelled by user")
                .build();

        handleProgressUpdate(calculationId, cancellation);
        completeSink(calculationId);
    }

    /**
     * Check if calculation exists
     */
    public boolean calculationExists(String calculationId) {
        return activeCalculations.containsKey(calculationId);
    }

    /**
     * Check if calculation is active (running)
     */
    public boolean isCalculationActive(String calculationId) {
        CalculationStatus status = activeCalculations.get(calculationId);
        return status != null &&
                (status.state() == CalculationState.STARTED ||
                        status.state() == CalculationState.CALCULATING);
    }

    /**
     * Get count of active calculations
     */
    public int getActiveCalculationCount() {
        return (int) activeCalculations.values().stream()
                .filter(s -> s.state() == CalculationState.STARTED ||
                        s.state() == CalculationState.CALCULATING)
                .count();
    }

    /**
     * Get optional status (for internal use)
     */
    public Optional<CalculationStatus> getStatus(String calculationId) {
        return Optional.ofNullable(activeCalculations.get(calculationId));
    }

    private void handleProgressUpdate(String calculationId, CalculationProgress progress) {
        // Send to WebSocket (legacy support)
        String destination = "/topic/calculation/" + calculationId;
        try {
            messagingTemplate.convertAndSend(destination, progress);
            log.debug("Sent {} update to WebSocket: {}", progress.type(), calculationId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update for calculation: {}", calculationId, e);
        }

        // Send to SSE sink
        Sinks.Many<CalculationProgress> sink = progressSinks.get(calculationId);
        if (sink != null) {
            Sinks.EmitResult result = sink.tryEmitNext(progress);
            if (result.isFailure()) {
                log.warn("Failed to emit SSE progress for {}: {}", calculationId, result);
            }
        }

        // Update calculation status
        if (progress instanceof CalculationProgress.Progress p) {
            updateCalculationState(calculationId, CalculationState.CALCULATING,
                    p.percentage(), p.phase());
        }
    }

    private void updateCalculationState(String calculationId, CalculationState state,
                                         Integer progress, String phase) {
        activeCalculations.computeIfPresent(calculationId, (id, status) ->
                new CalculationStatus(id, status.startTime(), state,
                        progress != null ? progress : status.progressPercentage(),
                        phase != null ? phase : status.currentPhase())
        );
    }

    private void completeSink(String calculationId) {
        Sinks.Many<CalculationProgress> sink = progressSinks.remove(calculationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    private void completeSinkWithError(String calculationId, Throwable error) {
        Sinks.Many<CalculationProgress> sink = progressSinks.remove(calculationId);
        if (sink != null) {
            sink.tryEmitError(error);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down calculation service...");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Complete all active sinks
        progressSinks.values().forEach(Sinks.Many::tryEmitComplete);
        progressSinks.clear();
    }

    /**
     * Internal calculation status record
     */
    public record CalculationStatus(
            String calculationId,
            long startTime,
            CalculationState state,
            Integer progressPercentage,
            String currentPhase
    ) {}
}
