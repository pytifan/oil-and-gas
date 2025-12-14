package com.oilgas.calculations.controller;

import com.oilgas.calculations.model.CalculationProgress;
import com.oilgas.calculations.model.CalculationRequest;
import com.oilgas.calculations.model.CalculationResponse;
import com.oilgas.calculations.model.api.CalculationStatusResponse;
import com.oilgas.calculations.model.api.ServiceInfoResponse;
import com.oilgas.calculations.service.CalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;

/**
 * REST API v1 for field work calculations.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Creating and managing calculations</li>
 *   <li>Streaming calculation progress via SSE</li>
 *   <li>Service information and capabilities</li>
 * </ul>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/calculations")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Calculations", description = "Oil & Gas field calculation operations")
public class CalculationsController {

    private final CalculationService calculationService;

    /**
     * Create a new calculation.
     *
     * <p>Initiates an asynchronous calculation and returns immediately with
     * the calculation ID. Use the SSE endpoint or WebSocket to receive progress updates.
     *
     * @param request the calculation request containing equations, parameters, and well configuration
     * @return 201 Created with Location header pointing to the calculation resource
     */
    @PostMapping
    @Operation(
            summary = "Create a new calculation",
            description = "Initiates an asynchronous calculation for oil & gas field operations. " +
                    "Returns immediately with calculation ID. Subscribe to SSE endpoint for progress updates."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Calculation created successfully",
                    headers = @Header(name = "Location", description = "URL to the created calculation resource"),
                    content = @Content(schema = @Schema(implementation = CalculationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many concurrent calculations",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<CalculationResponse> createCalculation(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Calculation request with equations, parameters, and well configuration",
                    required = true
            )
            CalculationRequest request
    ) {
        log.info("Received calculation request for {} equations", request.equations().size());

        if (request.wellConfig() != null) {
            log.info("Well info - Name: {}, Field: {}, Depth: {}m, Fluid: {}",
                    request.wellConfig().wellName(),
                    request.wellConfig().fieldName(),
                    request.wellConfig().depthMeters(),
                    request.wellConfig().fluidType());
        }

        String calculationId = calculationService.startCalculation(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(calculationId)
                .toUri();

        CalculationResponse response = CalculationResponse.builder()
                .calculationId(calculationId)
                .status("STARTED")
                .message("Calculation initiated successfully")
                .wsSubscriptionPath("/topic/calculation/" + calculationId)
                .sseStreamUrl("/api/v1/calculations/" + calculationId + "/progress")
                .estimatedTimeSeconds(30L)
                .build();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get calculation status.
     *
     * @param calculationId the unique calculation identifier
     * @return the current calculation status
     */
    @GetMapping("/{calculationId}")
    @Operation(
            summary = "Get calculation status",
            description = "Returns the current status and progress of a calculation"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Calculation status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CalculationStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Calculation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<CalculationStatusResponse> getCalculation(
            @Parameter(description = "Unique calculation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String calculationId
    ) {
        log.debug("Status request for calculation: {}", calculationId);
        CalculationStatusResponse status = calculationService.getCalculationStatus(calculationId);
        return ResponseEntity.ok(status);
    }

    /**
     * Cancel a running calculation.
     *
     * @param calculationId the unique calculation identifier
     * @return 204 No Content on success
     */
    @DeleteMapping("/{calculationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancel a calculation",
            description = "Cancels a running calculation. Has no effect on completed or failed calculations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Calculation cancelled successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Calculation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public void cancelCalculation(
            @Parameter(description = "Unique calculation identifier")
            @PathVariable String calculationId
    ) {
        log.info("Cancel request for calculation: {}", calculationId);
        calculationService.cancelCalculation(calculationId);
    }

    /**
     * Stream calculation progress via Server-Sent Events.
     *
     * <p>This endpoint provides real-time progress updates for a calculation.
     * The stream completes when the calculation finishes (success or error).
     *
     * @param calculationId the unique calculation identifier
     * @return SSE stream of progress updates
     */
    @GetMapping(value = "/{calculationId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream calculation progress",
            description = "Server-Sent Events stream for real-time calculation progress. " +
                    "Stream completes when calculation finishes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE stream established",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Calculation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public Flux<CalculationProgress> streamProgress(
            @Parameter(description = "Unique calculation identifier")
            @PathVariable String calculationId
    ) {
        log.debug("SSE subscription request for calculation: {}", calculationId);
        return calculationService.getProgressStream(calculationId);
    }

    /**
     * Get service information and capabilities.
     *
     * @return service configuration and available options
     */
    @GetMapping("/info")
    @Operation(
            summary = "Get service information",
            description = "Returns service capabilities, supported fluids, unit systems, and API endpoints"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service information retrieved successfully",
            content = @Content(schema = @Schema(implementation = ServiceInfoResponse.class))
    )
    public ResponseEntity<ServiceInfoResponse> getServiceInfo() {
        return ResponseEntity.ok(ServiceInfoResponse.defaults());
    }
}
