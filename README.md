# Search Microservice

Este repositorio implementa el **Search Microservice** del sistema de e-commerce orientado a microservicios.

## Arquitectura

El servicio sigue los principios de **Clean Architecture** y está separado en las siguientes capas:
- `SearchService.Domain`
- `SearchService.Application`
- `SearchService.Infrastructure`
- `SearchService.Presentation`

## Tecnologías Principales (Sprint 1)
- Docker & Docker Compose
- Redis (Caché)

*(Próximamente: RabbitMQ, Elasticsearch, Kong, Keycloak)*

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

3. Levanta los servicios base (Redis por ahora)
   ```bash
   docker-compose up -d
   ```

4. Verifica el estado
   ```bash
   docker-compose ps
   ```

5. Interactuar con Redis
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

- `src/` - Código fuente organizado por capas.
- `tests/` - Pruebas unitarias y de integración.
- `deploy/` - Archivos para despliegue (Docker, Kong, etc.).
- `docs/` - Documentación del proyecto.