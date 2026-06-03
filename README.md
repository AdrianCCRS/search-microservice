# Search Microservice

Search Microservice del sistema de e-commerce orientado a microservicios. Implementa el **read model CQRS**: consume eventos del Catalog, construye `SearchDocument`s, los indexa en Elasticsearch, cachea lecturas frecuentes en Redis y expone APIs de búsqueda a través de Kong API Gateway.

## Arquitectura

El servicio sigue los principios de **Clean Architecture** y se organiza en cuatro capas:

| Capa | Responsabilidad |
|---|---|
| `domain` | Entidades de dominio y contratos (sin dependencias de framework) |
| `application` | Casos de uso, orquestación, DTOs/eventos de aplicación, puertos de entrada/salida |
| `infrastructure` | Adaptadores: Redis, Elasticsearch, RabbitMQ, seguridad, cache |
| `presentation` | Controladores REST, DTOs HTTP, configuración y entrypoint |

**Patrón CQRS:** Este servicio es el lado de lectura. El Catalog es el source of truth transaccional y dueño de las escrituras.

### Multi-Service Ecosystem

Kong API Gateway unifica el routing hacia todos los microservicios del ecosistema:

```
                    ┌─────────────┐
                    │    Kong      │
                    │   :8000      │
                    └──┬──┬──┬──┬──┘
          ┌───────────┘  │  │  │  └────────────┐
          ▼              ▼  │  ▼               ▼
   ┌──────────┐  ┌──────────┐┌──────────┐ ┌──────────┐
   │ Frontend │  │  Search  ││ Catalog  │ │   Cart   │
   │   :80    │  │  :8080   ││  :5290   │ │  :8000   │
   └──────────┘  └──────────┘└──────────┘ └──────────┘
```

## Tecnologías

| Tecnología | Versión | Propósito |
|---|---|---|
| Java + Spring Boot | 17 + 3.1.5 | Runtime y framework base |
| Elasticsearch | 8.11.3 | Motor de búsqueda full-text |
| Redis | 7.0 | Caché de queries y sugerencias |
| RabbitMQ | 3.12 | Mensajería de eventos (Topic Exchange + DLQ) |
| Kong | 3.5 | API Gateway (DB-less, config declarativa) |
| Keycloak | 23.0 | Identity Provider (JWT RS256) |
| Prometheus | 2.51 | Métricas y scraping |
| Grafana | 10.4 | Dashboards de observabilidad |
| Nginx | Alpine | Servidor de la UI frontend |
| Maven | 3.9 | Build y gestión de dependencias |

## Servicios y Puertos

| Servicio | Contenedor | Puerto local | Puerto interno | Acceso |
|---|---|---|---|---|
| Kong API Gateway | `search_kong` | 8000, 8001 | 8000, 8001 | `http://localhost:8000` |
| Search API | `deploy-search-api-1` | 8085 | 8080 | `http://localhost:8085` |
| Frontend UI | `search_frontend` | — | 80 | `http://localhost:8000/` via Kong |
| Catalog API | `catalog_api` | 5290 | 5290 | `http://localhost:8000/api/v1/catalog` via Kong |
| Cart API | `cart_api` | — | 8000 | `http://localhost:8000/api/cart` via Kong |
| Keycloak | `search_keycloak` | 8081 | 8080 | `http://localhost:8081` |
| Elasticsearch | `search_elasticsearch` | 9200 | 9200 | `http://localhost:9200` |
| Redis | `search_redis` | 6379 | 6379 | `redis-cli` (autenticado) |
| RabbitMQ | `search_rabbitmq` | 5672, 15672 | 5672, 15672 | `http://localhost:15672` |
| Prometheus | `search_prometheus` | 9090 | 9090 | `http://localhost:9090` |
| Grafana | `search_grafana` | 3000 | 3000 | `http://localhost:3000` |

## API Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/search?q={query}&page=0&size=10` | JWT | Búsqueda full-text con paginación |
| `GET` | `/api/search/suggest?q={prefix}` | JWT | Sugerencias de autocompletado |
| `GET` | `/actuator/health` | — | Health check |
| `GET` | `/actuator/prometheus` | — | Métricas en formato Prometheus |

**Parámetros de búsqueda:**

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `q` | string | requerido | Texto de búsqueda (max 200 caracteres) |
| `page` | int | 0 | Página de resultados |
| `size` | int | 10 | Resultados por página |

> Las queries son sanitizadas automáticamente por `EsQuerySanitizer` antes de llegar a Elasticsearch.

## Flujo de Eventos CQRS

```
Catalog Service ──(publica eventos)──▶ RabbitMQ (exchange: catalog.events)
                                           │
                          ┌────────────────┼────────────────┐
                          ▼                                 ▼
           search.product.created              search.product.updated
                          │                                 │
                          ▼                                 ▼
           ProductCreatedConsumer             ProductUpdatedConsumer
           (indexa en Elasticsearch)          (invalida cache Redis)
                                                         │
                                              search.dead.letter (DLQ)
```

- **Consumidores** usan acknowledge manual (`AcknowledgeMode.MANUAL`)
- **Errores** son nackeados explícitamente hacia la DLQ `search.dead.letter`
- **Retry** con backoff exponencial configurado en `application.yml` (3 intentos, intervalo inicial 5s, multiplicador 3x)

## Estrategia de Cache (Redis)

| Tipo | Key pattern | TTL |
|---|---|---|
| Resultados de búsqueda | `search:query:{SHA-256(query normalizada)}` | 1 min |
| Sugerencias | `search:suggest:{query normalizada}` | 5 min |
| Producto individual | `search:product:{productId}` | — (invalidado por eventos) |

- **Normalización de query:** trim, colapso de espacios, lowercase
- **Invalidación:** el consumidor `ProductUpdatedConsumer` invalida tanto el cache del producto específico como todas las queries cacheadas que coincidan con los patrones de búsqueda y sugerencias

## Kong API Gateway — Rutas

| Ruta | Métodos | Servicio upstream | Auth |
|---|---|---|---|
| `/` | GET | Frontend (nginx:80) | Pública |
| `/api/search` | GET | Search API | JWT |
| `/api/search/suggest` | GET | Search API | JWT |
| `/api/v1/catalog` | GET, POST, PUT, PATCH, DELETE | Catalog API | Pública (con auth interna) |
| `/api/auth` | POST | Catalog API | Pública |
| `/images` | GET | Catalog API | Pública |
| `/health` | GET | Catalog API | Pública |
| `/catalog` | GET | Catalog Frontend | Pública |
| `/api/cart` | GET, POST, PUT, DELETE | Cart API | Pública (con auth interna) |

**Plugins activos en Search API:**
- `jwt` — Validación de firma RSA (Keycloak), claim `iss`, verificación de `exp`
- `rate-limiting` — 60 req/min por consumer (JWT) + 60 req/min por IP, política `local`, fault-tolerant

## Frontend

Incluye una UI de búsqueda (`frontend/`) servida con nginx, expuesta en `http://localhost:8000/` a través de Kong:

- Campo de búsqueda con autocompletado en tiempo real (endpoint `/api/search/suggest`)
- Resultados con nombre, descripción, categoría, precio, rating y marca
- Búsqueda por tecla Enter y botón, debounce de 250ms en sugerencias
- Diseño responsive sin frameworks externos (HTML5 + CSS3 + vanilla JS)

## Arranque Local

### Requisitos previos

- Docker y Docker Compose instalados
- Git

### Inicio rápido

```bash
git clone https://github.com/AdrianCCRS/search-microservice.git
cd search-microservice
cd deploy && cp .env.example .env && cd ..
docker compose -f deploy/docker-compose.yml up -d --build
```

> Keycloak tarda ~90 segundos en estar listo. Kong espera automáticamente a que Keycloak esté healthy.

### Verificar estado

```bash
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f search-api
```

### Acceder a servicios

| Servicio | URL | Credenciales |
|---|---|---|
| Frontend UI | `http://localhost:8000/` | — |
| Search API (directo) | `http://localhost:8085` | JWT Bearer |
| RabbitMQ Management | `http://localhost:15672` | guest / guest |
| Grafana | `http://localhost:3000` | admin / admin123 |
| Prometheus | `http://localhost:9090` | — |
| Keycloak Admin | `http://localhost:8081` | admin / admin |

### Interactuar con Redis

```bash
docker exec -it search_redis redis-cli
# > AUTH redis_secure_pass_123
# > PING
# PONG
```

## Autenticación JWT (Kong + Keycloak)

### Diagrama

```
Cliente → Kong :8000 → (valida JWT con clave RSA) → Search API :8080
                ↑
          Keycloak :8081
         (emite tokens)
```

### Obtener token y consumir la API

```bash
# Obtener token
TOKEN=$(curl -s -X POST http://localhost:8081/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=search-client" \
  -d "username=testuser" \
  -d "password=testpassword" | jq .access_token -r)

# Con token (200 OK)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8000/api/search?q=laptop"

# Sin token (401 Unauthorized)
curl -i "http://localhost:8000/api/search?q=laptop"
```

| Endpoint | Sin token | Con token válido |
|---|---|---|
| `GET /api/search?q=...` | 401 | 200 |
| `GET /api/search/suggest?q=...` | 401 | 200 |

> La Search API recibe requests ya validadas por Kong — no procesa JWT directamente.

### Nota sobre la clave RSA

La clave RSA en `deploy/kong/kong.yml` es generada por Keycloak al arrancar. Si usas `docker compose down`, Keycloak genera una nueva clave y debes actualizarla manualmente. Con `docker compose up -d` (sin `down`) los volúmenes persisten y la clave se mantiene.

## Observabilidad (Prometheus + Grafana)

Stack de monitoreo integrado en `docker-compose.yml`:

- **Prometheus:** Scrapea métricas de `search-api` (Actuator) y `elasticsearch-exporter`
- **Grafana:** Dashboard **"Search Microservice"** auto-provisionado con 7 paneles:
  - Latencia P95, cache hit rate Redis, QPS, tasa de errores, latencia Elasticsearch, tamaño de índice, duración de indexación
- **Alertas:** P95 > 100ms, cache hit rate < 70%
- **Elasticsearch Exporter:** Métricas de índices, shards y nodos ES

## GCP Deployment

Consulta la guía completa de despliegue en Google Cloud Platform:
[`docs/gcp-deployment-guide.md`](docs/gcp-deployment-guide.md)

Incluye:
- Compute Engine VM (`e2-standard-4`) con Docker Compose
- Setup completo: clonado, variables de entorno, build y despliegue
- Configuración de IP estática externa y firewall rules

## Security Policies

### Rate Limiting

Dos capas de rate limiting:

| Capa | Mecanismo | Configuración |
|---|---|---|
| Kong Gateway | Plugin `rate-limiting` por consumer + IP | 60 req/min, política `local` |
| Spring Boot | `RateLimitInterceptor` | `search.security.rate-limit.enabled` (default `true`), `search.security.rate-limit.requests-per-minute` (default `60`) |

Exceder el límite → HTTP 429.

### Input Validation

| Regla | Mecanismo | Respuesta |
|---|---|---|
| Query requerida | `@NotBlank` (Jakarta) | 400 |
| Query ≤ 200 chars | `@Size(max=200)` + chequeo programático | 400 |
| Page > 100 sin `searchAfter` | Chequeo programático | 400 |

### Elasticsearch DSL Injection Protection

- Queries construidas exclusivamente con `ElasticsearchClient` typed DSL builder
- `EsQuerySanitizer` remueve caracteres especiales de ES (`*`, `?`, `~`, `^`, `{`, `}`, `[`, `]`, etc.) como capa adicional de defensa

### Error Response Format

| Status | Escenario | Body |
|---|---|---|
| 400 | Parámetros inválidos | `{"error": "mensaje"}` |
| 429 | Rate limit excedido | `{"error": "Demasiadas solicitudes..."}` |
| 500 | Error interno | `{"error": "Error interno del servidor"}` |

## Testing

```bash
# Todos los tests (sin necesidad de Docker Compose)
mvn test

# Un test específico
mvn -Dtest=ProductCreatedConsumerTest test

# A través de Docker
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn test
```

Los tests son unitarios con Mockito; no requieren servicios externos.

## Documentación

| Documento | Descripción |
|---|---|
| [`docs/gcp-deployment-guide.md`](docs/gcp-deployment-guide.md) | Guía de despliegue en Google Cloud |
| [`docs/plan-pruebas-funcionales.md`](docs/plan-pruebas-funcionales.md) | Plan de pruebas funcionales (caja negra, 14 casos) |
| [`docs/sprints/walkthrough-T01-T02.md`](docs/sprints/walkthrough-T01-T02.md) | Walkthrough de JWT + Observabilidad |
| [`docs/sprint-4-tarea-3/summary.md`](docs/sprint-4-tarea-3/summary.md) | Resumen de implementación de seguridad |
| [`docs/sprints/sprint-1-implementations.md`](docs/sprints/sprint-1-implementations.md) | Implementaciones del Sprint 1 |
| [`AGENTS.md`](AGENTS.md) | Guía para agentes de IA que trabajen en el proyecto |
| [`deploy/`](deploy/) | Configuración de despliegue completa |

## Estructura del Repositorio

```
.
├── src/main/java/                  # Código fuente Java
│   ├── domain/entities/            # SearchDocument
│   ├── application/                # Use cases, events, ports
│   ├── infrastructure/             # Redis, ES, RabbitMQ, seguridad
│   └── presentation/               # Controllers, advice, app entrypoint
├── src/main/resources/             # application.yml
├── src/test/java/                  # Tests unitarios
├── frontend/                       # UI de búsqueda (nginx + static)
├── deploy/                         # Infraestructura como código
│   ├── docker-compose.yml          # Stack completo de servicios
│   ├── .env.example                # Template de variables de entorno
│   ├── kong/kong.yml               # API Gateway declarativo
│   ├── keycloak/realm-export.json  # Realm ecommerce
│   ├── prometheus/                 # Scraping + alertas
│   ├── grafana/                    # Dashboards + datasources
│   ├── elasticsearch/mappings/     # Mapping de search document
│   └── rabbitmq/                   # Definiciones de queues
├── docs/                           # Documentación
├── Dockerfile                      # Imagen del microservicio
├── pom.xml                         # Maven project descriptor
└── AGENTS.md                       # Instrucciones para agentes de IA
```
