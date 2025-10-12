package com.oilgas.calculations.controller;

import com.oilgas.calculations.model.CalculationRequest;
import com.oilgas.calculations.model.CalculationResponse;
import com.oilgas.calculations.service.CalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for field work calculations
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/calculations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CalculationsController {

    private final CalculationService calculationService;

    /**
     * Start a new calculation
     * POST /api/calculations/start
     */
    @PostMapping("/start")
    public ResponseEntity<CalculationResponse> startCalculation(
            @Valid @RequestBody CalculationRequest request
    ) {
        log.info("Received calculation request for {} equations",
                request.equations().size());

        if (request.wellConfig() != null) {
            log.info("Well info - Name: {}, Field: {}, Depth: {}m, Fluid: {}",
                    request.wellConfig().wellName(),
                    request.wellConfig().fieldName(),
                    request.wellConfig().depthMeters(),
                    request.wellConfig().fluidType());
        }

        String calculationId = calculationService.startCalculation(request);

        return ResponseEntity.accepted()
                .body(CalculationResponse.started(calculationId));
    }

    /**
     * Check calculation status
     * GET /api/calculations/status/{calculationId}
     */
    @GetMapping("/status/{calculationId}")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable String calculationId
    ) {
        boolean isActive = calculationService.isCalculationActive(calculationId);

        return ResponseEntity.ok(Map.of(
                "calculationId", calculationId,
                "active", isActive,
                "status", isActive ? "CALCULATING" : "COMPLETED_OR_NOT_FOUND"
        ));
    }

    /**
     * Health check
     * GET /api/calculations/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        int activeCalculations = calculationService.getActiveCalculationCount();

        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "Calculations Gateway",
                "version", "1.0.0",
                "activeCalculations", activeCalculations
        ));
    }

    /**
     * Service info
     * GET /api/calculations/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "service", "Oil & Gas Field Calculations",
                "description", "Calculate volumes for field operations",
                "supportedFluids", new String[]{
                        "drilling_mud",
                        "cement",
                        "completion_fluid",
                        "spacer_fluid",
                        "displacement_fluid"
                },
                "unitSystems", new String[]{"metric", "imperial"},
                "solverMethods", new String[]{"hybr", "lm", "broyden1"},
                "websocketEndpoint", "/ws",
                "calculationTopic", "/topic/calculation/{calculationId}"
        ));
    }
}