# Oil & Gas Calculations — Full System Context

## System Architecture
Three services orchestrated via docker-compose.yml in this repo:

| Service | Repo path | Docker service | Port(s) |
|---------|-----------|----------------|---------|
| Spring Boot Gateway | `.` (this repo) | `calculations-gateway` | 8080 |
| Python gRPC Service | `../calculator-python-service` | `python-calculator` | 50051 (gRPC), 8000 (health) |
| React Frontend | `../calculations-frontend` | `frontend` | 3000→80 |

Request flow: Browser → nginx (frontend:80) → `/api/*` proxy → Gateway:8080 → gRPC → Python:50051

## Key URLs (local dev)
- Frontend (Docker): http://localhost:3000
- Frontend (Vite dev): http://localhost:5173
- API: http://localhost:8080/api/v1/calculations
- Swagger: http://localhost:8080/swagger-ui.html
- Gateway health: http://localhost:8080/actuator/health
- Python health: http://localhost:8000/health

## Gateway (Java) — `calculations-gateway/`
- Java 25, Spring Boot 3.5.6, Maven (`./mvnw`)
- Spring MVC (NOT WebFlux) + SseEmitter for SSE
- gRPC client: grpc-spring-boot-starter → `grpc.client.python-calculator.address`
- Virtual threads: `Executors.newVirtualThreadPerTaskExecutor()`
- Resilience4j circuit breaker + retry on gRPC calls
- Proto: `src/main/proto/calculations.proto` — MUST stay in sync with `../calculator-python-service/proto/calculation.proto`
- Key classes: `CalculationsController`, `CalculationService`, `PythonCalculatorClient`
- DTOs: records; progress variants: sealed interface (`Progress | Result | Error`)
- Null-safe defaults: compact constructors in records

## Python Service — `../calculator-python-service/`
- Python 3.12, grpcio==1.75.1, numpy, scipy
- Entry: `src/main.py` — starts gRPC server on :50051, HTTP health on :8000
- Core physics: `src/well_calculator.py` (`WellCompletionCalculator`)
- Generated stubs: `src/calculation_pb2.py`, `src/calculation_pb2_grpc.py` (DO NOT edit manually)
- Tests: `src/test_calculator.py`, `src/test_service.py` (unit), `src/test_grpc_client.py` (integration, needs server)
- snake_case everywhere; dataclasses for domain models

## Frontend — `../calculations-frontend/`
- React + Vite + TypeScript + TailwindCSS
- Dev server: `npm run dev` → http://localhost:5173
- Build: `npm run build` → `dist/` → served by nginx in Docker
- Dev proxy: `/api` → `http://localhost:8080` (vite.config.ts)
- Docker nginx proxy: `/api/` → `http://calculations-gateway:8080`
- SSE: `src/hooks/useCalculationSSE.ts` using native `EventSource`
- Types: `src/types/calculation.ts`

## Architecture Decisions (DO NOT change without understanding why)

**SseEmitter, not Flux\<T\>:** Returning `Flux<T>` from a Spring MVC controller with `text/event-stream`
produces `Content-Length: 0` in this hybrid MVC+WebFlux setup. `SseEmitter(-1L)` works correctly.

**Replay sink kept alive:** `completeSink()` uses `progressSinks.get()` not `progressSinks.remove()`.
The sink stays in the map until `scheduleCalculationCleanup()` runs 5 minutes later.
This allows late SSE subscribers to replay all buffered events.

**publishOn(Schedulers.boundedElastic()):** Required in `getProgressStream()` so that replay
emission is async — prevents `SseEmitter.complete()` from firing before Spring initializes the response.

**grpcio-tools version pinned at 1.75.1:** Container has `grpcio==1.75.1`. Regenerating stubs
with a newer grpcio-tools version produces incompatible code (`requires grpcio>=1.78.0`).
Always regenerate with `pip install grpcio-tools==1.75.1 --break-system-packages`.

**Proto field numbers preserved:** `CalculationOptions.unit_system` kept at field=4 (not 1)
for wire format compatibility with existing clients.

## Available Skills (slash commands)

| Command | Description |
|---------|-------------|
| `/build` | Compile the gateway JAR (skip tests) |
| `/deploy` | Rebuild all Docker images + start all services |
| `/restart <service>` | Restart one service without rebuilding |
| `/logs <service>` | Tail logs for a service (last 50 lines) |
| `/test-api` | POST a test calculation + stream SSE progress events |
| `/python-test` | Run Python unit tests (no server needed) |
| `/proto` | Regenerate Python gRPC stubs — read IMPORTANT note inside |
| `/frontend-dev` | Start Vite dev server with hot-reload at :5173 |

Services: `calculations-gateway` · `python-calculator` · `frontend`

## Do Not
- Add back `equations`/`initialParameters` to `CalculationRequest` (fully removed from all layers)
- Use `Flux<T>` return type in Spring MVC SSE endpoints
- Regenerate Python stubs with `grpcio-tools` version != 1.75.1
- Call `progressSinks.remove()` from `completeSink()` (breaks late SSE subscribers)
- Edit `calculation_pb2.py` or `calculation_pb2_grpc.py` manually (generated files)
