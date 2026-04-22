# Copilot Context — Search Microservice (Proyecto E-commerce)

## 1. Contexto general del sistema

Este proyecto es un sistema de e-commerce basado en **arquitectura de microservicios**.

El sistema está diseñado bajo los siguientes principios:
- Arquitectura distribuida
- Comunicación asincrónica mediante eventos
- Separación de responsabilidades por dominio
- Uso de **CQRS (Command Query Responsibility Segregation)**

El sistema no es monolítico. Cada microservicio tiene una responsabilidad clara.

---

## 2. Microservicios del sistema

### Catalog Service
- Gestiona productos
- Es la **fuente de verdad (source of truth)**
- Usa base de datos (ej: MongoDB)
- Publica eventos:
  - `ProductCreated`
  - `ProductUpdated`

---

### Search Microservice (este repositorio)
- Es un **Read Model de CQRS**
- NO es fuente de verdad
- NO escribe datos transaccionales
- NO consulta directamente la base del Catalog

Responsabilidades:
- Consumir eventos desde RabbitMQ
- Transformar eventos a documentos de búsqueda
- Indexar en Elasticsearch
- Usar Redis como caché
- Exponer consultas de búsqueda vía API REST

---

### API Gateway (Kong)
- Punto de entrada del sistema
- Maneja:
  - routing
  - autenticación (JWT)
  - rate limiting

---

### Keycloak
- Identity Provider (IAM)
- Emite tokens JWT
- Kong delega autenticación aquí

---

## 3. Flujo de datos

### Write path (event-driven)
Catalog → RabbitMQ → Search → Elasticsearch → Redis (invalidate)

### Read path
Cliente → Kong → Search → Redis → Elasticsearch

---

## 4. Tecnologías principales

- Elasticsearch → motor de búsqueda
- Redis → caché
- RabbitMQ → mensajería
- Kong → API Gateway
- Keycloak → autenticación
- Docker Compose → entorno local

---

## 5. Estado actual del desarrollo

Estamos en **Sprint 1**.

### Progreso actual:
- D1 completado:
  - Docker Compose base
  - Redis funcional

### Trabajo actual:
- D2:
  - Configurar Elasticsearch
  - Definir mapping `SearchDocument`

---

## 6. SearchDocument (concepto clave)

El Search Microservice no almacena entidades de negocio completas.
Trabaja con un modelo optimizado para búsqueda:

### Campos esperados:
- productId
- name
- description
- category
- price
- rating
- available
- brand

Este documento es derivado de eventos, no de una base de datos directa.

---

## 7. Contrato de eventos

Eventos provenientes de Catalog:

### ProductCreated
### ProductUpdated

Formato esperado:

```json
{
  "eventType": "ProductCreated",
  "version": "1.0",
  "timestamp": "2026-03-25T10:00:00Z",
  "data": {
    "productId": "123",
    "name": "Zapatos Nike",
    "description": "Zapatos deportivos",
    "category": "calzado",
    "price": 99.9,
    "rating": 4.7,
    "available": true,
    "brand": "Nike"
  }
}