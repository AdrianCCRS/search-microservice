Quiero implementar D1 del Search Microservice.

Objetivo:
- Configurar Docker Compose base
- Configurar Redis

Contexto:
- Arquitectura de microservicios
- Search es el Read Model de CQRS
- Search consumirá eventos desde RabbitMQ
- Search indexará en Elasticsearch
- Redis se usará como caché
- Clean Architecture con capas Domain, Application, Infrastructure y Presentation

Necesito que propongas:
1. estructura inicial del repositorio;
2. archivo `deploy/docker-compose.yml`;
3. archivo `deploy/.env.example`;
4. README mínimo con instrucciones de arranque local.

Restricciones:
- no implementar RabbitMQ consumer todavía;
- no implementar Elasticsearch mapping todavía;
- no mezclar responsabilidades entre capas;
- no crear código fuera de la estructura definida;
- usar nombres claros y mantenibles;
- dejar preparado el terreno para D2 y D3.