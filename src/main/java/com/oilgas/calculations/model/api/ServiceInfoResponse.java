package com.oilgas.calculations.model.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * Response for service information endpoint
 */
@Builder
@Schema(description = "Service capabilities and configuration information")
public record ServiceInfoResponse(
        @Schema(description = "Service name")
        String service,

        @Schema(description = "API version")
        String apiVersion,

        @Schema(description = "Service description")
        String description,

        @Schema(description = "Supported fluid types for calculations")
        List<String> supportedFluids,

        @Schema(description = "Available unit systems")
        List<String> unitSystems,

        @Schema(description = "Available solver methods")
        List<String> solverMethods,

        @Schema(description = "API endpoints information")
        EndpointsInfo endpoints,

        @Schema(description = "Service limits and constraints")
        LimitsInfo limits
) {
    @Builder
    @Schema(description = "Available API endpoints")
    public record EndpointsInfo(
            @Schema(description = "REST API base path")
            String restApi,

            @Schema(description = "WebSocket endpoint for legacy clients")
            String websocket,

            @Schema(description = "SSE progress stream path template")
            String progressStream,

            @Schema(description = "OpenAPI documentation URL")
            String documentation
    ) {}

    @Builder
    @Schema(description = "Service operational limits")
    public record LimitsInfo(
            @Schema(description = "Maximum concurrent calculations")
            int maxConcurrentCalculations,

            @Schema(description = "Calculation timeout in seconds")
            int calculationTimeoutSeconds,

            @Schema(description = "Maximum equations per request")
            int maxEquationsPerRequest
    ) {}

    public static ServiceInfoResponse defaults() {
        return ServiceInfoResponse.builder()
                .service("Oil & Gas Field Calculations Gateway")
                .apiVersion("v1")
                .description("Calculate volumes for field operations including drilling, cementing, and completion")
                .supportedFluids(List.of(
                        "drilling_mud",
                        "cement",
                        "completion_fluid",
                        "spacer_fluid",
                        "displacement_fluid"
                ))
                .unitSystems(List.of("metric", "imperial"))
                .solverMethods(List.of("hybr", "lm", "broyden1"))
                .endpoints(EndpointsInfo.builder()
                        .restApi("/api/v1/calculations")
                        .websocket("/ws")
                        .progressStream("/api/v1/calculations/{calculationId}/progress")
                        .documentation("/swagger-ui.html")
                        .build())
                .limits(LimitsInfo.builder()
                        .maxConcurrentCalculations(100)
                        .calculationTimeoutSeconds(300)
                        .maxEquationsPerRequest(50)
                        .build())
                .build();
    }
}
