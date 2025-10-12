package com.oilgas.calculations.model;

import java.util.List;

// WebSocket messages
public sealed interface ProgressMessage {
    String requestId();

    String type();

    record Progress(
            String requestId,
            String type,
            int percentage,
            String phase,
            int iteration,
            double residual,
            String message
    ) implements ProgressMessage {
        public Progress(String requestId, int percentage, String phase,
                        int iteration, double residual, String message) {
            this(requestId, "progress", percentage, phase, iteration, residual, message);
        }
    }

    record Result(
            String requestId,
            String type,
            List<Double> solution,
            Metadata metadata
    ) implements ProgressMessage {
        public Result(String requestId, List<Double> solution, Metadata metadata) {
            this(requestId, "result", solution, metadata);
        }
    }

    record Error(
            String requestId,
            String type,
            String code,
            String message
    ) implements ProgressMessage {
        public Error(String requestId, String code, String message) {
            this(requestId, "error", code, message);
        }
    }

    record Metadata(
            String algorithm,
            int iterations,
            double finalResidual,
            long elapsedMs,
            boolean converged
    ) {
    }
}
