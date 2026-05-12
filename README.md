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

## Flujo de Autenticación JWT (Kong + Keycloak)

Se implementó autenticación JWT utilizando Kong como API Gateway (plugin estático JWT) y Keycloak como Identity Provider.

### Arquitectura

1. **Cliente (`search-client`)**: Aplicación/Frontend o herramienta de testing que interactúa con Keycloak para obtener el JWT.
2. **Identity Provider (`Keycloak`)**: Emite el JWT tras validar usuario/contraseña.
3. **API Gateway (`Kong`)**: Recibe la petición con el JWT, valida la firma RSA de forma estática con el plugin `jwt`, y redirige al Search API interno si es válido.
4. **Microservicio (`Search API`)**: Recibe la petición en el backend, no necesita gestionar desencriptación JWT (lo asume validado por Kong). Su cliente en Keycloak (`search-service`) está configurado como `bearerOnly`.

¿Por qué Kong OSS con plugin estático JWT en lugar de OpenID-Connect?
Kong Community Edition (OSS) no dispone del plugin dinámico `openid-connect`. Como alternativa, usamos el plugin `jwt` indicándole estáticamente la clave RSA pública del issuer (Keycloak) y validando el claim `iss` localmente.

### Obtener Token y Endpoints Protegidos

**Rutas protegidas:**
- `GET /api/search`
- `GET /api/search/suggest`

Cualquier petición sin un token válido retornará `401 Unauthorized`.

Para obtener el token (con las credenciales por defecto configuradas en el realm export):
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=search-client" \
  -d "username=testuser" \
  -d "password=testpassword" | jq .access_token -r)
```

**Llamada a Endpoint Exitoso (200 OK):**
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/search?q=laptop
```

**Llamada a Endpoint Fallido (401 Unauthorized):**
```bash
curl -i http://localhost:8000/api/search?q=laptop
```

## Estado Actual de la Implementación

**Fase 1 completada (Preparación de infraestructura y configuración):**
- ✅ `realm-export.json`: Configurado cliente `search-client` (Public) y conservado `search-service` (Bearer Only).
- ✅ `kong.yml`: Configurado el plugin `jwt` con un placeholder explícito (`<SE_EXTRAERA_DE_KEYCLOAK_EN_EJECUCION_FASE_2>`) para la clave RSA pública.
- ✅ `docker-compose.yml`: Añadido `depends_on: keycloak` en el servicio `kong`, y mapeadas las variables necesarias. Observabilidad (Prometheus, Grafana, exporters) se mantiene completamente intacta.
- ✅ `.env.example`: Agregadas variables de Kong y Keycloak manteniendo las variables de observabilidad existentes.

**⚠️ Fase 2 pendiente (Runtime y pruebas End-to-End):**
Esta implementación **NO ha sido probada en runtime** porque requiere infraestructura Docker no disponible en esta fase. Queda pendiente:
1. Levantar contenedores Docker (`docker compose up -d`).
2. Esperar que Keycloak esté saludable.
3. Extraer la clave RSA pública real del endpoint JWKS (`http://localhost:8081/realms/ecommerce/protocol/openid-connect/certs`).
4. Reemplazar el placeholder `<SE_EXTRAERA_DE_KEYCLOAK_EN_EJECUCION_FASE_2>` en `deploy/kong/kong.yml` con la clave RSA pública real.
5. Reiniciar Kong (`docker compose restart kong`).
6. Ejecutar curl probando token nulo (401) y válido (200).
7. Validar que el campo `iss` emitido coincida con `http://keycloak:8080/realms/ecommerce` (si no, ajustar el issuer en la clave `key` dentro de la configuración de consumer en `kong.yml`).

## Impacto en Despliegue Docker (Fase 2 Esperada)

- **Nuevo flujo**: Ahora Kong esperará activamente a que Keycloak se reporte saludable antes de iniciar.
- **Observabilidad Intacta**: Las métricas de Prometheus hacia el exporter y spring-boot no se ven afectadas.
- **Comandos esperados para validación (NO EJECUTAR AÚN)**:
  ```bash
  docker compose up -d
  # ... Esperar que levanten servicios ...
  # (Sustituir RSA pub key manualmente o por script)
  docker compose restart kong
  ```

**Riesgos Pendientes:**
- El **issuer real** del JWT debe coincidir con la URL que Kong evalúa. Si Keycloak usa hostnames diferentes por la red interna/externa (ej. `localhost:8081` vs `keycloak:8080`), esto deberá ser alineado en `kong.yml`.
- La **clave RSA** estática (placeholder actual) requiere ser cambiada durante runtime inicial, un mismatch generará un `401 Unauthorized`.
- Kong no se reiniciará automáticamente si la configuración cambia. Debe usarse `docker compose restart kong` tras cambiar `kong.yml`.
- Posibles conflictos en puertos (`8000`, `8001`, `8081`) dependiendo de la disponibilidad del host local al momento de ejecutar Fase 2.
