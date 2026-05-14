#!/usr/bin/env bash
set -euo pipefail

ELASTICSEARCH_HOST=${ELASTICSEARCH_HOST:-localhost}
ELASTICSEARCH_PORT=${ELASTICSEARCH_PORT:-9200}
ELASTICSEARCH_INDEX=${ELASTICSEARCH_INDEX:-products}
WAIT_TIMEOUT=${WAIT_TIMEOUT:-120}
WAIT_INTERVAL=${WAIT_INTERVAL:-5}

log() { printf '%s\n' "$1"; }
error() { printf 'ERROR: %s\n' "$1" >&2; exit 1; }

wait_for_elasticsearch() {
  log "Esperando a Elasticsearch en http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}..."
  local start_time=$(date +%s)

  while true; do
    if curl -sSf "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}" >/dev/null 2>&1; then
      log "Elasticsearch disponible."
      return
    fi

    local now=$(date +%s)
    if (( now - start_time > WAIT_TIMEOUT )); then
      error "No se pudo conectar a Elasticsearch en ${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT} después de ${WAIT_TIMEOUT}s."
    fi

    printf '.'
    sleep "$WAIT_INTERVAL"
  done
}

ensure_index_exists() {
  local head_code
  head_code=$(curl -s -o /dev/null -w '%{http_code}' "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}/${ELASTICSEARCH_INDEX}")

  if [ "$head_code" = "200" ]; then
    log "Índice '${ELASTICSEARCH_INDEX}' ya existe."
    return
  fi

  log "Creando índice '${ELASTICSEARCH_INDEX}' con mapping base..."
  local create_code
  create_code=$(curl -s -o /dev/null -w '%{http_code}' \
    -X PUT "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}/${ELASTICSEARCH_INDEX}" \
    -H 'Content-Type: application/json' \
    -d '{"mappings":{"properties":{"productId":{"type":"keyword"},"name":{"type":"text","fields":{"keyword":{"type":"keyword","ignore_above":256}}},"description":{"type":"text"},"category":{"type":"keyword"},"price":{"type":"double"},"rating":{"type":"float"},"available":{"type":"boolean"},"brand":{"type":"keyword"}}}}')

  if [ "$create_code" != "200" ] && [ "$create_code" != "201" ]; then
    curl -sSf "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}/${ELASTICSEARCH_INDEX}" >/dev/null 2>&1 || true
    error "Fallo al crear índice '${ELASTICSEARCH_INDEX}'. Código HTTP: ${create_code}."
  fi

  log "Índice '${ELASTICSEARCH_INDEX}' creado correctamente."
}

prepare_bulk_payload() {
  local payload_file="$1"
  shift

  categories=("Laptops" "Smartphones" "Audio" "Hogar" "Oficina" "Deportes" "Gaming" "Cámaras" "Wearables" "Electrónica")
  brands=("Acme" "Nova" "Zenith" "Orion" "Pixel" "Hyperion" "Falcon" "Vega" "Atlas" "Nexus")
  model_types=("Pro" "Max" "Elite" "Prime" "Turbo" "Ultra" "Plus" "Core" "Flex" "Edge")
  item_names=("Laptop" "Phone" "Headphones" "Monitor" "Keyboard" "Mouse" "Speaker" "Tablet" "Camera" "Watch")

  log "Generando payload NDJSON para ${ELASTICSEARCH_INDEX} con 100 documentos..."
  : > "$payload_file"

  for i in $(seq 1 100); do
    category="${categories[$((i % ${#categories[@]}))]}"
    brand="${brands[$((i % ${#brands[@]}))]}"
    model="${model_types[$((i % ${#model_types[@]}))]}"
    item="${item_names[$((i % ${#item_names[@]}))]}"
    product_id="prod-$i"
    price=$(awk "BEGIN { printf \"%.2f\", 49.99 + (($i * 13) % 950) + 0.99 }")
    rating=$(awk "BEGIN { printf \"%.1f\", 3.0 + (($i % 20) * 0.1) }")
    available=$([ $((i % 4)) -ne 0 ] && printf 'true' || printf 'false')
    name="${brand} ${item} ${model}"
    description="${name} con características premium, batería de larga duración y conectividad avanzada para el consumidor moderno."

    cat <<EOF >> "$payload_file"
{"index":{"_index":"${ELASTICSEARCH_INDEX}","_id":"${product_id}"}}
{"productId":"${product_id}","name":"${name}","description":"${description}","category":"${category}","price":${price},"rating":${rating},"available":${available},"brand":"${brand}"}
EOF
  done
}

verify_bulk_response() {
  local response_file="$1"
  if grep -q '"errors"\s*:\s*true' "$response_file"; then
    error "La respuesta Bulk API devolvió errores. Revisa '$response_file' para detalles."
  fi
}

count_documents() {
  local response total
  response=$(curl -sSf "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}/${ELASTICSEARCH_INDEX}/_count")
  total=$(printf '%s' "$response" | grep -o '"count"[[:space:]]*:[[:space:]]*[0-9]\+' | head -n 1 | tr -cd '0-9')
  if [ -z "$total" ]; then
    error "No se pudo parsear el conteo de documentos desde Elasticsearch. Respuesta: $response"
  fi
  printf '%s' "$total"
}

main() {
  log "--- Seed de datos para pruebas de carga ---"
  wait_for_elasticsearch
  ensure_index_exists

  local payload_file
  payload_file=$(mktemp)
  # Expand path now: EXIT runs after main returns, so a trap using "$payload_file" would hit set -u.
  trap "rm -f '${payload_file}'" EXIT

  prepare_bulk_payload "$payload_file"

  log "Indexando documentos usando Bulk API..."
  local bulk_response
  bulk_response=$(mktemp)
  curl -sSf -X POST "http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}/${ELASTICSEARCH_INDEX}/_bulk?refresh=true" \
    -H 'Content-Type: application/x-ndjson' \
    --data-binary "@$payload_file" > "$bulk_response"

  verify_bulk_response "$bulk_response"
  rm -f "$bulk_response"

  local total
  total=$(count_documents)
  if [ "$total" -lt 100 ]; then
    error "Conteo final de documentos insuficiente: esperado 100, obtenido ${total}."
  fi

  log "Seed completado con éxito. Documentos indexados: ${total}."
  log "Puedes ejecutar una búsqueda de verificación con:"
  log "  curl -s 'http://localhost:9200/${ELASTICSEARCH_INDEX}/_search?q=laptop' | jq '.'"
}

main "$@"
