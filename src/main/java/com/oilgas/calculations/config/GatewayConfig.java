package com.oilgas.calculations.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway configuration properties
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app.gateway")
public class GatewayConfig {

    private String defaultUnitSystem = "metric";
    private int maxConcurrentCalculations = 100;
    private int calculationTimeoutSeconds = 300;

    public void logConfiguration() {
        log.info("Gateway Config - Unit System: {}, Max Concurrent: {}, Timeout: {}s",
                defaultUnitSystem, maxConcurrentCalculations, calculationTimeoutSeconds);
    }
}