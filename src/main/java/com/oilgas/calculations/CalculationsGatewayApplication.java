package com.oilgas.calculations;

import com.oilgas.calculations.config.GatewayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Oil & Gas Calculations Gateway
 *
 * REST API and WebSocket interface for real-time field calculations
 * Communicates with Python calculator service via gRPC
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(GatewayConfig.class)
@RequiredArgsConstructor
public class CalculationsGatewayApplication implements CommandLineRunner {

    private final GatewayConfig gatewayConfig;

    public static void main(String[] args) {
        SpringApplication.run(CalculationsGatewayApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("╔═══════════════════════════════════════════════════╗");
        log.info("║  Calculations Gateway Started Successfully       ║");
        log.info("╚═══════════════════════════════════════════════════╝");
        log.info("");
        log.info("🛢️  Service: Oil & Gas Field Operations");
        log.info("🌐 REST API: http://localhost:8080/api/calculations");
        log.info("🔌 WebSocket: ws://localhost:8080/ws");
        log.info("📊 Health: http://localhost:8080/api/calculations/health");
        log.info("ℹ️  Info: http://localhost:8080/api/calculations/info");
        log.info("");

        gatewayConfig.logConfiguration();

        log.info("");
        log.info("Ready to calculate! 🚀");
    }
}