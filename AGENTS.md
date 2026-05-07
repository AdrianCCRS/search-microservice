# AGENTS.md

## Project context
- Search Microservice for an e-commerce microservices system.
- This service is the CQRS read model: it consumes Catalog product events, builds `SearchDocument`s, indexes them in Elasticsearch, caches frequent reads in Redis, and exposes search APIs through Kong.
- Do not treat this service as the transactional source of truth; Catalog owns product writes.

## AI agent operational mode
- Work in build mode unless the user explicitly asks for planning, review, or analysis only.
- Agents are permitted to make file changes, run shell commands, and use available tools to complete requested implementation tasks.
- Prefer Docker-based commands for validation in this repository when local tooling such as Maven is unavailable.

## Scope and stack
- Single-module Maven project (`pom.xml`), Java 17, Spring Boot 3.1.5.
- Main class: `presentation.SearchApplication` (component scan: `presentation`, `infrastructure`, `application`, `domain`).
- Real code layout is Java packages under `src/main/java` and tests under `src/test/java`; ignore outdated `.NET-style` folder examples in older docs.

## Source-of-truth commands
- Run tests: `mvn test`
- Run tests through Docker: `docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn test`
- Run one test class: `mvn -Dtest=ProductCreatedConsumerTest test`
- Run one test class through Docker: `docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn -Dtest=ProductCreatedConsumerTest test`
- Build jar: `mvn clean package`
- Build through Docker: `docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn clean package`
- Run app locally without Docker: `mvn spring-boot:run`
- Full local environment: `docker compose -f deploy/docker-compose.yml up -d --build`
- Check API logs in compose: `docker compose -f deploy/docker-compose.yml logs -f search-api`

## Architecture rules
- `domain`: domain entities/contracts only; avoid framework dependencies here.
- `application`: use cases, orchestration, application DTOs/events, input/output ports.
- `infrastructure`: Redis, Elasticsearch, RabbitMQ, persistence/integration adapters, Kong/Keycloak deploy config.
- `presentation`: REST controllers, HTTP-facing DTOs/configuration, app entrypoint.
- `SearchDocument` is derived from events and should contain at least `productId`, `name`, `description`, `category`, `price`, `rating`, `available`, and `brand`.

## Runtime wiring that agents often miss
- API container is exposed on host port `8085`; app listens on `8080` inside the container.
- Search endpoints are `GET /api/search?q=...` and `GET /api/search/suggest?q=...`.
- RabbitMQ exchange/queues are predeclared in both Spring config and Rabbit definitions:
  - exchange `catalog.events`
  - queues `search.product.created`, `search.product.updated`, `search.dead.letter`
- Rabbit listeners use manual ack/nack; failures are explicitly nacked to DLQ.
- Redis is password-protected in local compose (`REDIS_PASSWORD`, default `redis_secure_pass_123`).
- Kong routes `/api/search` and `/api/search/suggest` to `search-api:8080`.

## Elasticsearch and cache quirks
- `ElasticsearchSearchRepository` currently hardcodes index name `products`.
- `ElasticsearchIndexInitializer` uses `es.index.name` with default `products`; keep both aligned if changing index names.
- `deploy/elasticsearch/mappings/search_document.json` exists, but startup currently creates only the index/settings and does not automatically apply that mapping file.
- Cache key strategy:
  - search keys: `search:query:` + SHA-256(normalized query)
  - suggest keys: `search:suggest:` + normalized query
  - normalization trims, collapses spaces, and lowercases.

## Testing expectations
- Current tests are unit-style Mockito tests; no compose services required for `mvn test`.
- Prefer targeted test runs for edited areas, then full `mvn test`.

## Branch and commit conventions
- Branches: `main` stable, `develop` integration, `feature/{hu-id}-{descripcion}`, `fix/{descripcion}`.
- Existing examples use Spanish HU branch names such as `feature/hu-05-entorno-base` and `feature/hu-05-configuracion-redis`.
- Example commit format from project instructions: `[HU-05] add redis service configuration`.

## Instruction-file precedence
- `.github/copilot-instructions.md` and `.github/contexts/copilot-context.md` are useful for domain context and workflow conventions.
- When docs conflict with executable config/classes, trust the current Java/Maven repo and runtime configuration.
