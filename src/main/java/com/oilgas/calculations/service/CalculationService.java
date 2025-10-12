package com.oilgas.calculations.service;

import com.oilgas.calculations.model.CalculationProgress;
import com.oilgas.calculations.model.CalculationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main service for managing calculations
 * Uses Java 25 Virtual Threads
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationService {

    private final PythonCalculatorClient pythonClient;
    private final SimpMessagingTemplate messagingTemplate;

    // Java 25 Virtual Threads
    private final ExecutorService virtualThreadExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    private final ConcurrentHashMap<String, CalculationStatus> activeCalculations =
            new ConcurrentHashMap<>();

    /**
     * Start a new calculation
     */
    public String startCalculation(CalculationRequest request) {
        String calculationId = UUID.randomUUID().toString();

        log.info("Starting calculation: {} for well: {}",
                calculationId,
                request.wellConfig() != null ? request.wellConfig().wellName() : "N/A");

        activeCalculations.put(calculationId, new CalculationStatus(
                calculationId,
                System.currentTimeMillis(),
                CalculationState.STARTED
        ));

        CompletableFuture.runAsync(() -> {
            try {
                pythonClient.calculate(
                        calculationId,
                        request,
                        progress -> handleProgressUpdate(calculationId, progress)
                ).join();

                updateCalculationStatus(calculationId, CalculationState.COMPLETED);

            } catch (Exception e) {
                log.error("Calculation failed: {}", calculationId, e);

                handleProgressUpdate(calculationId, CalculationProgress.Error.builder()
                        .calculationId(calculationId)
                        .type("error")
                        .errorCode("CALCULATION_FAILED")
                        .errorMessage(e.getMessage())
                        .suggestion("Check input parameters and try again")
                        .build());

                updateCalculationStatus(calculationId, CalculationState.FAILED);
            }
        }, virtualThreadExecutor);

        return calculationId;
    }

    private void handleProgressUpdate(String calculationId, CalculationProgress progress) {
        String destination = "/topic/calculation/" + calculationId;

        try {
            messagingTemplate.convertAndSend(destination, progress);

            log.debug("Sent {} update to WebSocket: {}", progress.type(), calculationId);

            if (progress instanceof CalculationProgress.Progress) {
                updateCalculationStatus(calculationId, CalculationState.CALCULATING);
            }

        } catch (Exception e) {
            log.error("Failed to send WebSocket update for calculation: {}",
                    calculationId, e);
        }
    }

    private void updateCalculationStatus(String calculationId, CalculationState state) {
        activeCalculations.computeIfPresent(calculationId, (id, status) ->
                new CalculationStatus(id, status.startTime(), state)
        );
    }

    public int getActiveCalculationCount() {
        return (int) activeCalculations.values().stream()
                .filter(s -> s.state() == CalculationState.CALCULATING)
                .count();
    }

    public boolean isCalculationActive(String calculationId) {
        CalculationStatus status = activeCalculations.get(calculationId);
        return status != null &&
                (status.state() == CalculationState.STARTED ||
                        status.state() == CalculationState.CALCULATING);
    }

    private record CalculationStatus(
            String calculationId,
            long startTime,
            CalculationState state
    ) {}

    private enum CalculationState {
        STARTED,
        CALCULATING,
        COMPLETED,
        FAILED
    }
}