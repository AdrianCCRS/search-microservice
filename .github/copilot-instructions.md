# Copilot Instructions — Search Microservice

## Contexto del proyecto
Este repositorio implementa el **Search Microservice** del sistema de e-commerce orientado a microservicios.
El servicio forma parte del **lado de lectura (Read Model)** bajo un enfoque **CQRS**.
Su responsabilidad es:
- consumir eventos desde RabbitMQ (`ProductCreated`, `ProductUpdated`);
- transformar esos eventos a un documento de búsqueda (`SearchDocument`);
- indexar en Elasticsearch;
- usar Redis como caché para consultas frecuentes;
- exponer la API detrás de Kong;
- delegar autenticación/autorización a Kong + Keycloak.

## Objetivo actual del sprint
Estamos desarrollando **D2** del Sprint 1 (El D1 con Docker Compose base y Redis ha sido completado).

### Tareas actuales
1. Configurar Elasticsearch
2. Definir mapping base de `SearchDocument`

### Modelo de Datos (SearchDocument)
El documento derivado de eventos (no almacenar entidades transaccionales completas) debe contener al menos:
- `productId`, `name`, `description`, `category`, `price`, `rating`, `available`, `brand`

Todavía **no** implementar:
- lógica completa de búsqueda;
- consumers RabbitMQ;
- Kong con configuración avanzada;
- integración completa con Keycloak;
- observabilidad;
- performance tuning.

## Convenciones de ramas
Asume esta estrategia Git:
- `main`: estable
- `develop`: integración
- `feature/{hu-id}-{descripcion}`
- `fix/{descripcion}`

Para este trabajo, usar ramas tipo:
- `feature/hu-05-entorno-base`
- `feature/hu-05-configuracion-redis`

Cuando generes ejemplos de commits, usa formato:
- `[HU-05] add base docker compose`
- `[HU-05] add redis service configuration`

## Convenciones de arquitectura
Usar **Clean Architecture** con estas capas:

- `SearchService.Domain`
- `SearchService.Application`
- `SearchService.Infrastructure`
- `SearchService.Presentation`

### Responsabilidades por capa
#### Domain
Solo reglas del dominio, entidades, contratos e interfaces.
No dependencias de frameworks.

#### Application
Casos de uso, orquestación, DTOs de aplicación, puertos de entrada/salida.

#### Infrastructure
Implementaciones técnicas:
- Redis
- Elasticsearch
- RabbitMQ
- Kong-related configs
- Keycloak-related configs
- persistencia e integraciones externas

#### Presentation
API REST, health endpoints, configuración HTTP, DTOs expuestos al exterior.

## Estructura esperada del repositorio
Copilot debe respetar esta estructura y proponer archivos dentro de ella:

```text
search-microservice/
├─ src/
│  ├─ SearchService.Domain/
│  ├─ SearchService.Application/
│  ├─ SearchService.Infrastructure/
│  └─ SearchService.Presentation/
├─ tests/
│  ├─ SearchService.Domain.Tests/
│  ├─ SearchService.Application.Tests/
│  ├─ SearchService.Infrastructure.Tests/
│  └─ SearchService.Presentation.Tests/
├─ deploy/
│  ├─ docker-compose.yml
│  ├─ .env.example
│  ├─ kong/
│  ├─ elasticsearch/
│  ├─ rabbitmq/
│  └─ keycloak/
├─ docs/
│  ├─ sprint-1.md
│  ├─ architecture.md
│  └─ decisions/
├─ README.md
└─ .gitignore