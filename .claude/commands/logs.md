Tail logs from running services. Pass a service name to filter:
- `calculations-gateway` — Spring Boot gateway
- `python-calculator` — Python gRPC service
- `frontend` — nginx + React

!/Applications/Docker.app/Contents/Resources/bin/docker compose -f /Users/oleksiiprokopenko/workspace/calculations-gateway/docker-compose.yml logs --tail=50 $ARGUMENTS
