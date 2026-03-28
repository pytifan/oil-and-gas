# Oil & Gas Field Calculations — System Overview

Three-service system for field job liquid volume calculations:

| Service | Repo | Role |
|---|---|---|
| **calculations-gateway** | [pytifan/oil-and-gas](https://github.com/pytifan/oil-and-gas) | Spring Boot REST gateway (port 8080) |
| **calculator-python-service** | [pytifan/calculator-python-service](https://github.com/pytifan/calculator-python-service) | Python gRPC solver (port 50051) |
| **calculations-frontend** | [pytifan/calculations-frontend](https://github.com/pytifan/calculations-frontend) | React UI with well visualization (port 3000 / 5173) |

---

## Option 1 — Docker Compose (recommended)

Runs all three services in containers with a single command.

### 1. Clone all repos into the right layout

```bash
git clone https://github.com/pytifan/oil-and-gas.git
cd oil-and-gas
git clone https://github.com/pytifan/calculator-python-service.git python-service
git clone https://github.com/pytifan/calculations-frontend.git frontend
```

Expected directory layout:
```
oil-and-gas/
  docker-compose.yml
  Dockerfile              ← gateway
  src/                    ← gateway source
  python-service/         ← cloned calculator-python-service
  frontend/               ← cloned calculations-frontend
```

### 2. Start everything

```bash
docker compose up --build
```

Services start in order: Python calculator → Gateway → Frontend.

### 3. Open the UI

**http://localhost:3000**

| URL | Description |
|---|---|
| http://localhost:3000 | React UI |
| http://localhost:8080/swagger-ui.html | Gateway API docs |
| http://localhost:8080/actuator/health | Gateway health |
| http://localhost:8000/health | Python calculator health |

---

## Option 2 — Local development (3 terminals)

Run each service locally for faster development iteration.

### Prerequisites

- Java 25
- Python 3.12+
- Node.js 20+

### Terminal 1 — Python Calculator

```bash
git clone https://github.com/pytifan/calculator-python-service.git
cd calculator-python-service

python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

python src/main.py
```

Listening on `localhost:50051` (gRPC).

### Terminal 2 — Calculations Gateway

```bash
git clone https://github.com/pytifan/oil-and-gas.git
cd oil-and-gas

./mvnw spring-boot:run
```

Listening on `http://localhost:8080`.

> The gateway connects to the Python service at `localhost:50051`. Start the Python service first — the circuit breaker retries 3 times before failing.

### Terminal 3 — Frontend

```bash
git clone https://github.com/pytifan/calculations-frontend.git
cd calculations-frontend

npm install
npm run dev
```

Opens at **http://localhost:5173**. The Vite dev server proxies `/api/*` to `localhost:8080` automatically.

---

## Verify everything is working

### 1. Python calculator health

```bash
# gRPC (from calculator-python-service/src/)
python test_grpc_client.py
```

### 2. Gateway health

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

### 3. Submit a test calculation via curl

```bash
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{
    "equations": ["x**2 + y**2 - 1", "x - y"],
    "initialParameters": [1.0, 1.0],
    "options": {
      "solverMethod": "hybr",
      "maxIterations": 1000,
      "tolerance": 1e-8,
      "unitSystem": "metric"
    }
  }'
```

Copy the `calculationId` from the response, then stream progress:

```bash
curl -N http://localhost:8080/api/v1/calculations/{calculationId}/progress
```

### 4. Open the UI

Navigate to **http://localhost:3000** (Docker) or **http://localhost:5173** (local dev).

Fill in equations, click **Run Calculation**, and watch the well bore fill with liquid in real time.

---

## Architecture

```
Browser
  │  REST + SSE
  ▼
calculations-gateway (Spring Boot :8080)
  │  gRPC
  ▼
calculator-python-service (Python :50051)
```

- The frontend calls the gateway REST API and subscribes to the SSE progress stream.
- The gateway delegates all equation solving to the Python service over gRPC.
- The Python service uses SciPy (`hybr`, `lm`, `fsolve`) to solve non-linear equation systems.
- Results flow back: Python → gateway → SSE stream → animated well UI.
