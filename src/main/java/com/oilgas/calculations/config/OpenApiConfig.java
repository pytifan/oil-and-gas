package com.oilgas.calculations.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI calculationsGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Calculations Gateway API")
                        .description("""
                                REST API for Oil & Gas field calculations.

                                ## Overview
                                This gateway provides endpoints for initiating and monitoring calculations
                                for field operations including drilling, cementing, and completion fluid volumes.

                                ## Real-time Updates
                                Progress updates are available via:
                                - **SSE (recommended)**: Subscribe to `/api/v1/calculations/{id}/progress`
                                - **WebSocket (legacy)**: Connect to `/ws` and subscribe to `/topic/calculation/{id}`

                                ## Error Handling
                                All errors follow RFC 9457 Problem Details format.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@oilgas.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://oilgas.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.oilgas.com")
                                .description("Production server")
                ));
    }
}
