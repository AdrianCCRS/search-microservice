#!/bin/sh
set -e

MAX_WAIT=120
WAIT_INTERVAL=3

wait_for_tcp() {
  host="$1"
  port="$2"
  label="$3"
  elapsed=0
  echo "Waiting for $label ($host:$port)..."
  while [ $elapsed -lt $MAX_WAIT ]; do
    if nc -z "$host" "$port" 2>/dev/null; then
      echo "$label is ready (took ${elapsed}s)"
      return 0
    fi
    sleep $WAIT_INTERVAL
    elapsed=$((elapsed + WAIT_INTERVAL))
  done
  echo "ERROR: $label ($host:$port) did not become ready within ${MAX_WAIT}s" >&2
  exit 1
}

wait_for_tcp "${REDIS_HOST:-redis}"       "${REDIS_PORT:-6379}"  "Redis"
wait_for_tcp "${ELASTICSEARCH_HOST:-elasticsearch}" "${ELASTICSEARCH_PORT:-9200}" "Elasticsearch"
wait_for_tcp "${RABBITMQ_HOST:-rabbitmq}" "${RABBITMQ_PORT:-5672}" "RabbitMQ"

echo "All infrastructure ready. Starting search-microservice..."
exec java -jar app.jar
