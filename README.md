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

- `src/main/java/` - Codigo fuente Spring Boot organizado por capas.
- `src/main/resources/` - Configuracion de la aplicacion.
- `src/test/java/` - Pruebas unitarias y de integracion.
- `Dockerfile` - Imagen principal del microservicio Search.
- `deploy/` - Archivos para despliegue (Docker, Kong, etc.).
- `docs/` - Documentación del proyecto.
