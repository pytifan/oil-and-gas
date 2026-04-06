Rebuild all Docker images and start all services (gateway + python-calculator + frontend):

!/Applications/Docker.app/Contents/Resources/bin/docker compose -f /Users/oleksiiprokopenko/workspace/calculations-gateway/docker-compose.yml build 2>&1 | tail -5

!/Applications/Docker.app/Contents/Resources/bin/docker compose -f /Users/oleksiiprokopenko/workspace/calculations-gateway/docker-compose.yml up -d 2>&1
