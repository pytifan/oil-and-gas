package com.oilgas.calculations.service;

import com.oilgas.calculations.exception.ServiceUnavailableException;
import com.oilgas.calculations.grpc.CalculationServiceGrpc;
import com.oilgas.calculations.grpc.CalculationsProto;
import com.oilgas.calculations.model.CalculationProgress;
import com.oilgas.calculations.model.CalculationRequest;
import com.oilgas.calculations.model.CalculationResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * gRPC client for Python calculator service.
 * Includes circuit breaker and retry patterns for resilience.
 */
@Slf4j
@Service
public class PythonCalculatorClient {

    private static final String CIRCUIT_BREAKER_NAME = "pythonCalculator";

    @GrpcClient("python-calculator")
    private CalculationServiceGrpc.CalculationServiceStub asyncStub;

    /**
     * Start calculation with streaming progress updates.
     * Protected by circuit breaker and retry patterns.
     *
     * @param calculationId unique calculation identifier
     * @param request the calculation request
     * @param progressConsumer consumer for progress updates
     * @return CompletableFuture that completes when calculation finishes
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "calculateFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Void> calculate(
            String calculationId,
            CalculationRequest request,
            Consumer<CalculationProgress> progressConsumer
    ) {
        log.info("Starting gRPC calculation stream: {}", calculationId);

        CompletableFuture<Void> future = new CompletableFuture<>();

        var grpcRequest = buildGrpcRequest(calculationId, request);

        asyncStub.calculate(grpcRequest, new StreamObserver<>() {
            @Override
            public void onNext(CalculationsProto.CalculationUpdate update) {
                try {
                    CalculationProgress progress = convertToProgress(calculationId, update);
                    progressConsumer.accept(progress);
                } catch (Exception e) {
                    log.error("Error processing calculation update", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC stream error for calculation: {}", calculationId, t);
                progressConsumer.accept(CalculationProgress.Error.builder()
                        .calculationId(calculationId)
                        .type("error")
                        .errorCode("GRPC_ERROR")
                        .errorMessage(t.getMessage())
                        .suggestion("Check Python calculator service connectivity")
                        .build());
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                log.info("gRPC stream completed for calculation: {}", calculationId);
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * Fallback method when circuit breaker is open or all retries exhausted.
     */
    @SuppressWarnings("unused")
    private CompletableFuture<Void> calculateFallback(
            String calculationId,
            CalculationRequest request,
            Consumer<CalculationProgress> progressConsumer,
            Throwable throwable
    ) {
        log.warn("Circuit breaker fallback triggered for calculation: {}. Reason: {}",
                calculationId, throwable.getMessage());

        progressConsumer.accept(CalculationProgress.Error.builder()
                .calculationId(calculationId)
                .type("error")
                .errorCode("SERVICE_UNAVAILABLE")
                .errorMessage("Calculation service is temporarily unavailable")
                .suggestion("The service is experiencing issues. Please try again in a few moments.")
                .build());

        return CompletableFuture.failedFuture(
                new ServiceUnavailableException("Python Calculator", throwable));
    }

    private CalculationsProto.CalculationRequest buildGrpcRequest(
            String calculationId,
            CalculationRequest request
    ) {
        var builder = CalculationsProto.CalculationRequest.newBuilder()
                .setCalculationId(calculationId)
                .addAllEquations(request.equations())
                .addAllInitialParameters(request.initialParameters())
                .setOptions(CalculationsProto.CalculationOptions.newBuilder()
                        .setSolverMethod(request.options().solverMethod())
                        .setMaxIterations(request.options().maxIterations())
                        .setTolerance(request.options().tolerance())
                        .setUnitSystem(request.options().unitSystem())
                        .build());

        if (request.wellConfig() != null) {
            var wellConfig = request.wellConfig();
            builder.setWellConfig(CalculationsProto.WellConfiguration.newBuilder()
                    .setWellName(wellConfig.wellName())
                    .setFieldName(wellConfig.fieldName())
                    .setDepthMeters(wellConfig.depthMeters())
                    .setDiameterInches(wellConfig.diameterInches())
                    .setFluidType(wellConfig.fluidType())
                    .build());
        }

        return builder.build();
    }

    private CalculationProgress convertToProgress(
            String calculationId,
            CalculationsProto.CalculationUpdate update
    ) {
        return switch (update.getUpdateCase()) {
            case PROGRESS -> {
                var p = update.getProgress();
                yield CalculationProgress.Progress.builder()
                        .calculationId(calculationId)
                        .type("progress")
                        .percentage(p.getPercentage())
                        .phase(p.getPhase())
                        .iteration(p.getIteration())
                        .convergenceMetric(p.getConvergenceMetric())
                        .message(p.getMessage())
                        .build();
            }

            case RESULT -> {
                var r = update.getResult();
                var volumes = r.getVolumesList().stream()
                        .map(v -> CalculationResult.VolumeRequirement.builder()
                                .fluidType(v.getFluidType())
                                .volumeM3(v.getVolumeM3())
                                .volumeBbl(v.getVolumeBbl())
                                .volumeGal(v.getVolumeGal())
                                .calculationBasis(v.getCalculationBasis())
                                .build())
                        .collect(Collectors.toList());

                var meta = r.getMetadata();
                var metadata = CalculationResult.CalculationMetadata.builder()
                        .algorithmUsed(meta.getAlgorithmUsed())
                        .iterations(meta.getIterations())
                        .finalConvergence(meta.getFinalConvergence())
                        .elapsedMs(meta.getElapsedMs())
                        .converged(meta.getConverged())
                        .unitSystem(meta.getUnitSystem())
                        .build();

                yield CalculationProgress.Result.builder()
                        .calculationId(calculationId)
                        .type("result")
                        .volumes(volumes)
                        .metadata(metadata)
                        .build();
            }

            case ERROR -> {
                var e = update.getError();
                yield CalculationProgress.Error.builder()
                        .calculationId(calculationId)
                        .type("error")
                        .errorCode(e.getErrorCode())
                        .errorMessage(e.getErrorMessage())
                        .build();
            }

            default -> throw new IllegalStateException(
                    "Unknown update type: " + update.getUpdateCase()
            );
        };
    }
}