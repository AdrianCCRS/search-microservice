# Search Microservice

Este repositorio implementa el **Search Microservice** del sistema de e-commerce orientado a microservicios.

## Arquitectura

El servicio sigue los principios de **Clean Architecture** y está separado en las siguientes capas:
- `domain`
- `application`
- `infrastructure`
- `presentation`

## Tecnologías Principales (Sprint 1)
- Java 17 & Spring Boot 3.1
- Docker & Docker Compose
- Redis 7.0 (Caché)
- RabbitMQ 3.12 (Mensajería dirigida por eventos - Topic Exchange & DLQ)
- Elasticsearch 8.11 (Motor principal de búsqueda)
- MongoDB 6.0 (Base de datos del servicio de lectura)

Tambien incluye Kong como API Gateway y Keycloak como proveedor de identidad para el entorno local.

## Arranque Local

1. Clona el repositorio
   ```bash
   git clone <repo-url>
   cd search-microservice
   ```

2. Configura las variables de entorno
   ```bash
   cd deploy
   cp .env.example .env
   ```
   *Nota: edita `.env` con las contraseñas requeridas, por defecto en local puedes usar los valores de ejemplo.*

3. Levanta los servicios completos y compila la API
   ```bash
   cd deploy
   docker compose up -d --build
   ```
   **Este comando:**
   - Levantará los contenedores de Redis, MongoDB, Elasticsearch y RabbitMQ.
   - Compilará tu código Java Spring Boot con Maven usando un contenedor de primera fase.
   - Creará y arrancará el contenedor `deploy-search-api` (expuesto en puerto local `8085`).

4. Verifica el estado
   ```bash
   docker compose ps
   ```
   O supervisa los logs en tiempo real de la API de Search:
   ```bash
   docker compose logs -f search-api
   ```

5. Interactuar con RabbitMQ (Mensajería)
   - Panel de Control Web: `http://localhost:15672` (Usuario: `guest`, Password: `guest`)
   - Revisa aquí las colas `search.product.created` y `search.product.updated`.

6. Interactuar con Redis
   Para comprobar que Redis se está ejecutando y requiere autenticación, puedes entrar a su CLI:
   ```bash
   docker exec -it search_redis redis-cli
   ```
   Una vez dentro, ingresa la contraseña definida en tu `.env` (o la por defecto `redis_secure_pass_123`):
   ```text
   127.0.0.1:6379> AUTH redis_secure_pass_123
   OK
   127.0.0.1:6379> PING
   PONG
   ```

## Estructura del Repositorio

- `src/main/java/` — Código fuente Spring Boot organizado por capas
- `src/main/resources/` — Configuración de la aplicación
- `src/test/java/` — Pruebas unitarias y de integración
- `Dockerfile` — Imagen principal del microservicio Search
- `deploy/` — Archivos para despliegue:
  - `docker-compose.yml` — Todos los servicios del entorno
  - `kong/kong.yml` — Configuración declarativa de Kong (rutas + JWT)
  - `keycloak/realm-export.json` — Realm ecommerce con usuarios y clientes
  - `prometheus/` — Configuración de scraping y alertas
  - `grafana/` — Dashboards y datasources auto-provisionados
- `docs/` — Documentación del proyecto

## Flujo de Autenticación JWT (Kong + Keycloak)

Se implementó autenticación JWT utilizando Kong como API Gateway (plugin estático JWT) y Keycloak como Identity Provider. **La implementación está completamente validada en runtime.**

### Arquitectura

```
Cliente → Kong :8000 → (valida JWT con clave RSA) → Search API :8080
                ↑
          Keycloak :8081
         (emite tokens)
```

1. **`search-client`** — Cliente público de Keycloak para obtener tokens (`grant_type=password`)
2. **`search-service`** — Cliente bearer-only (solo valida tokens, no los emite)
3. **Kong** — Valida la firma RSA del JWT con el plugin `jwt` nativo (Kong OSS no incluye `openid-connect`)
4. **Search API** — Recibe requests ya validadas; no necesita procesar JWT directamente

### Rutas protegidas

| Endpoint | Sin token | Con token válido |
|---|---|---|
| `GET /api/search?q=...` | 401 Unauthorized | 200 OK |
| `GET /api/search/suggest?q=...` | 401 Unauthorized | 200 OK |

### Obtener token y consumir la API

```bash
# 1. Obtener token de Keycloak
TOKEN=$(curl -s -X POST http://localhost:8081/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=search-client" \
  -d "username=testuser" \
  -d "password=testpassword" | jq .access_token -r)

# 2. Llamada exitosa con token (200 OK)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8000/api/search?q=laptop"

# 3. Llamada sin token (401 Unauthorized)
curl -i "http://localhost:8000/api/search?q=laptop"
```

---

## Observabilidad (Prometheus + Grafana)

El stack de monitoreo está integrado en el mismo `docker-compose.yml`.

| Servicio | URL | Credenciales |
|---|---|---|
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus | http://localhost:9090 | — |
| Actuator métricas | http://localhost:8085/actuator/prometheus | — |

El dashboard **"Search Microservice"** se provisiona automáticamente en Grafana con 7 paneles:
latencia P95, cache hit rate Redis, QPS, tasa de errores, latencia Elasticsearch e índice ES.

---

## Guía de Despliegue para el Equipo

> **Lee esto antes de empezar cualquier tarea del sprint.**

### Requisitos previos

- Docker Desktop instalado y corriendo
- Git configurado con tu cuenta de GitHub
- PowerShell o bash disponible

### Paso 1 — Clonar y posicionarse en la rama correcta

```bash
git clone https://github.com/AdrianCCRS/search-microservice.git
cd search-microservice
git checkout feauture/T-01-JWT-kong+keycloak   # rama del sprint actual
```

### Paso 2 — Configurar variables de entorno

```bash
cd deploy
cp .env.example .env
# No necesitas cambiar nada para entorno local; los valores por defecto funcionan
```

### Paso 3 — Levantar el entorno completo

```bash
# Desde la raíz del proyecto:
docker compose -f deploy/docker-compose.yml up -d --build
```

Este comando levanta **todos** los servicios:
`search-api`, `redis`, `mongodb`, `elasticsearch`, `rabbitmq`, `keycloak`, `kong`, `elasticsearch-exporter`, `prometheus`, `grafana`

> ⚠️ **Keycloak tarda ~90 segundos en estar listo.** Kong espera automáticamente a que esté healthy antes de arrancar.

### Paso 4 — Verificar que todo está running

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Todos los servicios deben aparecer como `Up (healthy)` o `Up`.

### Paso 5 — Verificar la autenticación JWT

```bash
# Debe retornar 401:
curl -i http://localhost:8000/api/search?q=test

# Obtener token y probar con auth:
TOKEN=$(curl -s -X POST http://localhost:8081/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password&client_id=search-client&username=testuser&password=testpassword" \
  | jq .access_token -r)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8000/api/search?q=test"
```

### Paso 6 — Verificar Grafana

Abre http://localhost:3000 → Login con `admin` / `admin123` → Busca el dashboard **"Search Microservice"**.

### Ver logs de la API

```bash
docker compose -f deploy/docker-compose.yml logs -f search-api
```

### Detener el entorno

```bash
docker compose -f deploy/docker-compose.yml down
# Para también eliminar volúmenes (datos):
docker compose -f deploy/docker-compose.yml down -v
```

---

## ⚠️ Nota sobre la clave RSA en kong.yml

La clave RSA pública en `deploy/kong/kong.yml` es la **clave real generada por Keycloak** en el momento del despliegue de este sprint. Keycloak genera una nueva clave cada vez que arranca con `KC_DB=dev-mem` (base de datos en memoria).

**Esto significa:** si borras el contenedor de Keycloak (`docker compose down`) y lo vuelves a levantar, Keycloak generará una clave RSA diferente y Kong comenzará a rechazar todos los tokens con `401`.

**Para regenerar la clave después de un `docker compose down`:**

```bash
# 1. Levanta los servicios sin Kong
docker compose -f deploy/docker-compose.yml up -d --no-deps keycloak

# 2. Espera ~90s a que Keycloak esté listo, luego extrae la nueva clave
# En PowerShell:
$jwks = (Invoke-WebRequest -Uri "http://localhost:8081/realms/ecommerce/protocol/openid-connect/certs" -UseBasicParsing | ConvertFrom-Json).keys | Where-Object { $_.use -eq "sig" }
# Luego actualiza la clave en deploy/kong/kong.yml y reinicia Kong

# O usa el script de validación incluido en docs/
```

> 💡 **Para el sprint:** si usas `docker compose up -d` (sin `down`) los volúmenes persisten y la clave no cambia. El problema solo ocurre al hacer `docker compose down`.

---

## Security Policies

### Rate Limiting

**Kong Gateway (primary):** The `rate-limiting` plugin is applied on both `search-route` and `suggest-route`, enforcing limits by Consumer (JWT) and IP address simultaneously. 

| Setting | Value | Environment Variable |
|---------|-------|---------------------|
| Requests per minute | 60 | `KONG_RATE_LIMIT_MINUTE` |
| Policy | `local` | — |
| Fault tolerant | `true` | — |

When the limit is exceeded, Kong returns `429 Too Many Requests`.

**Spring Boot (defense-in-depth):** A `RateLimitInterceptor` provides a secondary rate limit layer at the application level, configurable via:
- `search.security.rate-limit.enabled` (default: `true`)
- `search.security.rate-limit.requests-per-minute` (default: `60`)

Uses `X-Forwarded-For` header (set by Kong) for client identification, falling back to `RemoteAddr`.

### Input Validation

| Constraint | Annotation | Error Response |
|-----------|-----------|---------------|
| Query must not be blank | `@NotBlank` | 400 Bad Request |
| Query length ≤ 200 characters | `@Size(max=200)` | 400 Bad Request |
| Page > 100 requires `searchAfter` | manual check | 400 Bad Request |

Validation is enforced through both Spring Bean Validation (`@Validated` + Jakarta annotations) and programmatic checks in the controller for defense-in-depth.

### Elasticsearch DSL Injection Protection

All ES queries use the `co.elastic.clients.elasticsearch.ElasticsearchClient` typed DSL builder (`multi_match` for search, `match_phrase_prefix` for suggestions). User input is never concatenated into raw query strings.

**Additional defense-in-depth:** An `EsQuerySanitizer` strips ES-special characters (`*`, `?`, `~`, `^`, `{`, `}`, `[`, `]`, `(`, `)`, `:`, `\`, `/`, `"`, `+`, `-`, `=`, `>`, `<`, `!`, `&`, `|`) from user queries before they reach Elasticsearch. This prevents wildcard expansion, fuzziness manipulation, and query DSL injection attempts even if the query builder approach were accidentally changed in the future.

### Error Response Format

| HTTP Status | Scenario | Response Body |
|-------------|----------|---------------|
| 400 | Invalid/missing parameters | `{"error": "<message>"}` |
| 429 | Rate limit exceeded | `{"error": "Demasiadas solicitudes..."}` |
| 500 | Internal errors | `{"error": "Error interno del servidor"}` |
