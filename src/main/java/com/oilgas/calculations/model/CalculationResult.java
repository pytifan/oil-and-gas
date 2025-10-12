package com.oilgas.calculations.model;

import lombok.Builder;

import java.util.List;

/**
 * Complete calculation result
 */
@Builder
public record CalculationResult(
        String calculationId,
        List<VolumeRequirement> volumes,
        CalculationMetadata metadata,
        Summary summary
) {

    /**
     * Individual fluid volume requirement
     */
    @Builder
    public record VolumeRequirement(
            String fluidType,
            double volumeM3,
            double volumeBbl,
            double volumeGal,
            String calculationBasis,
            String description
    ) {
        public String getFormattedVolume(String unitSystem) {
            return switch (unitSystem.toLowerCase()) {
                case "metric" -> String.format("%.2f m³", volumeM3);
                case "imperial" -> String.format("%.2f bbl (%.2f gal)", volumeBbl, volumeGal);
                default -> String.format("%.2f m³ / %.2f bbl", volumeM3, volumeBbl);
            };
        }
    }

    /**
     * Calculation metadata
     */
    @Builder
    public record CalculationMetadata(
            String algorithmUsed,
            int iterations,
            double finalConvergence,
            long elapsedMs,
            boolean converged,
            String unitSystem
    ) {
        public String getFormattedTime() {
            if (elapsedMs < 1000) {
                return elapsedMs + " ms";
            } else {
                return String.format("%.2f s", elapsedMs / 1000.0);
            }
        }
    }

    /**
     * Summary of total volumes
     */
    @Builder
    public record Summary(
            double totalVolumeM3,
            double totalVolumeBbl,
            double totalVolumeGal,
            int fluidTypesCount,
            String primaryFluidType
    ) {}
}