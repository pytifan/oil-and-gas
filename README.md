# Calculations Gateway

Spring Boot gateway for Oil & Gas field calculations. Accepts calculation requests via REST, streams progress via SSE or WebSocket, and delegates the actual solving to a Python calculator service over gRPC.

## Prerequisites

- Java 25 (uses `./mvnw` — no local Maven install required)
- Python calculator service running on `localhost:50051` (gRPC)

## Start the server

```bash
./mvnw spring-boot:run
```

Server starts on **http://localhost:8080**.

## Verify everything works

### 1. Health check

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

### 2. Service info

```bash
curl http://localhost:8080/api/v1/calculations/info
```

Returns supported fluids, unit systems, and API endpoints.

### 3. Submit a calculation

```bash
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{
    "equations": ["x + y = 10", "x - y = 2"],
    "initialParameters": [1.0, 1.0],
    "options": {
      "solverMethod": "hybr",
      "maxIterations": 1000,
      "tolerance": 1e-8,
      "unitSystem": "metric"
    }
  }'
```

Returns a `calculationId` and a `Location` header. Example response:

```json
{
  "calculationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "STARTED",
  "sseStreamUrl": "/api/v1/calculations/550e8400.../progress"
}
```

> **Note:** Calculations are delegated to the Python gRPC service. If it isn't running, the request will fail with a circuit breaker error after 3 retries.

### 4. Stream progress (SSE)

```bash
curl -N http://localhost:8080/api/v1/calculations/{calculationId}/progress
```

### 5. Check calculation status

```bash
curl http://localhost:8080/api/v1/calculations/{calculationId}
```

### 6. Cancel a calculation

```bash
curl -X DELETE http://localhost:8080/api/v1/calculations/{calculationId}
```

## API docs

Interactive Swagger UI: **http://localhost:8080/swagger-ui.html**

OpenAPI JSON: **http://localhost:8080/v3/api-docs**

## Other actuator endpoints

| Endpoint | Description |
|---|---|
| `/actuator/health` | Health status |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |

## Running all services (Docker)

The `docker-compose.yml` defines three services — this gateway, the Python calculator, and a React frontend. To run all of them together:

```bash
docker compose up
```

| Service | Port |
|---|---|
| Calculations Gateway (REST) | 8080 |
| Python Calculator (gRPC) | 50051 |
| Python Calculator (health) | 8000 |
| Frontend | 3000 |

> The `python-service/` and `frontend/` source directories live in separate repositories and must be present alongside this one for Docker Compose to build successfully.
