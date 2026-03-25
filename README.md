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

## Estructura del Repositorio

- `src/` - Código fuente organizado por capas.
- `tests/` - Pruebas unitarias y de integración.
- `deploy/` - Archivos para despliegue (Docker, Kong, etc.).
- `docs/` - Documentación del proyecto.