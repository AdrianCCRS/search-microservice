# Plan de Pruebas Funcionales — Search Microservice

## 1. Objetivo

Validar que los endpoints expuestos por el Search Microservice a través de Kong API Gateway cumplen con los requerimientos funcionales definidos. Las pruebas se ejecutan en modalidad de **caja negra**: se invocan los endpoints vía HTTP y se verifica la respuesta sin conocer la implementación interna.

## 2. Alcance

| Endpoint | Método | Autenticación |
|---|---|---|
| `GET /api/search?q=...` | GET | JWT (Bearer) |
| `GET /api/search/suggest?q=...` | GET | JWT (Bearer) |
| `GET /api/auth/login` | POST | Pública |

## 3. Escenario de prueba

- Servicios desplegados con Docker Compose en servidor OCI.
- Kong en `localhost:8000`.
- Catálogo con 20 vehículos de demostración precargados (`POST /api/v1/catalog/products/bulk/demo`).
- Token JWT obtenido vía `POST /api/auth/login` con credenciales `admin / admin123`.

---

## 4. Casos de prueba

### TC-01: Búsqueda full-text exitosa (RF-01)

| Atributo | Valor |
|---|---|
| **ID** | TC-01 |
| **RF** | RF-01 — Búsqueda full-text |
| **Descripción** | Buscar un producto existente por nombre |
| **Precondición** | Catálogo con datos cargados, token JWT válido |
| **Entrada** | `GET /api/search?q=Toyota` con `Authorization: Bearer <token>` |
| **Resultado esperado** | Código `200`. Respuesta JSON con lista de `SearchDocument`. Al menos un documento cuyo `name` contenga "Toyota" |
| **Resultado obtenido** | ✅ `200 OK`. Se devuelven 3 documentos: Toyota Corolla, Toyota Hilux, Toyota Yaris |

### TC-02: Búsqueda con query vacía (RF-01)

| Atributo | Valor |
|---|---|
| **ID** | TC-02 |
| **RF** | RF-01 — Búsqueda full-text |
| **Descripción** | Enviar búsqueda sin el parámetro `q` o con `q` vacío |
| **Entrada** | `GET /api/search` (sin `q`) |
| **Resultado esperado** | Código `400`. Mensaje de error indicando que `q` es requerido |
| **Resultado obtenido** | ✅ `400 Bad Request`. `{"error": "El parámetro 'q' es requerido"}` |

### TC-03: Búsqueda con query demasiado larga (RF-01)

| Atributo | Valor |
|---|---|
| **ID** | TC-03 |
| **RF** | RF-01 — Búsqueda full-text |
| **Descripción** | Enviar query de más de 200 caracteres |
| **Entrada** | `GET /api/search?q=<string de 250 caracteres>` |
| **Resultado esperado** | Código `400`. Mensaje de error indicando tamaño máximo |
| **Resultado obtenido** | ✅ `400 Bad Request`. `{"error": "El parámetro 'q' no debe exceder 200 caracteres"}` |

### TC-04: Búsqueda con tolerancia a errores ortográficos — Fuzzy Search (RF-06)

| Atributo | Valor |
|---|---|
| **ID** | TC-04 |
| **RF** | RF-06 — Fuzzy search |
| **Descripción** | Buscar "Toyote" en lugar de "Toyota" y verificar que igual devuelva resultados de Toyota |
| **Entrada** | `GET /api/search?q=Toyote` con token |
| **Resultado esperado** | Código `200`. La respuesta incluye productos Toyota a pesar del error tipográfico |
| **Resultado obtenido** | ✅ `200 OK`. Se devuelven los mismos 3 productos Toyota. El `fuzziness("AUTO")` de Elasticsearch toleró el error |

### TC-05: Búsqueda con múltiples errores (RF-06)

| Atributo | Valor |
|---|---|
| **ID** | TC-05 |
| **RF** | RF-06 — Fuzzy search |
| **Descripción** | Buscar "Corola" en lugar de "Corolla" |
| **Entrada** | `GET /api/search?q=Corola` con token |
| **Resultado esperado** | Código `200`. Se devuelve "Toyota Corolla" a pesar de la omisión de una "l" |
| **Resultado obtenido** | ✅ `200 OK`. Documento Toyota Corolla presente en resultados |

### TC-06: Búsqueda sin resultados

| Atributo | Valor |
|---|---|
| **ID** | TC-06 |
| **RF** | RF-01 — Búsqueda full-text |
| **Descripción** | Buscar un término que no existe en el índice |
| **Entrada** | `GET /api/search?q=ZzzNoExiste123` con token |
| **Resultado esperado** | Código `200`. Lista vacía `[]` |
| **Resultado obtenido** | ✅ `200 OK`. `[]` |

### TC-07: Sugerencias — Autocompletado (RF-04)

| Atributo | Valor |
|---|---|
| **ID** | TC-07 |
| **RF** | RF-04 — Autocompletado |
| **Descripción** | Escribir prefijo de un producto y recibir sugerencias |
| **Entrada** | `GET /api/search/suggest?q=Toy` con token |
| **Resultado esperado** | Código `200`. Lista de strings con nombres de productos que comienzan con "Toy". Ej: `["Toyota Corolla", "Toyota Hilux", "Toyota Yaris"]` |
| **Resultado obtenido** | ✅ `200 OK`. 3 sugerencias con nombres de vehículos Toyota |

### TC-08: Sugerencias sin query (RF-04)

| Atributo | Valor |
|---|---|
| **ID** | TC-08 |
| **RF** | RF-04 — Autocompletado |
| **Descripción** | Enviar sugerencia sin el parámetro `q` |
| **Entrada** | `GET /api/search/suggest` (sin `q`) |
| **Resultado esperado** | Código `400`. Mensaje de error |
| **Resultado obtenido** | ✅ `400 Bad Request`. `{"error": "El parámetro 'q' es requerido"}` |

### TC-09: Sugerencias sin resultados

| Atributo | Valor |
|---|---|
| **ID** | TC-09 |
| **RF** | RF-04 — Autocompletado |
| **Descripción** | Prefijo que no coincide con ningún nombre |
| **Entrada** | `GET /api/search/suggest?q=Zzzz` con token |
| **Resultado esperado** | Código `200`. Lista vacía `[]` |
| **Resultado obtenido** | ✅ `200 OK`. `[]` |

### TC-10: Ranking por relevancia (RF-07)

| Atributo | Valor |
|---|---|
| **ID** | TC-10 |
| **RF** | RF-07 — Ranking por relevancia |
| **Descripción** | Buscar "Chevrolet" y verificar que el primer resultado tenga "Chevrolet" en el nombre (no solo en la descripción) |
| **Entrada** | `GET /api/search?q=Chevrolet` con token |
| **Resultado esperado** | Código `200`. El primer documento tiene `name` que comienza con "Chevrolet" (por el field boosting `name^3`) |
| **Resultado obtenido** | ✅ `200 OK`. Chevrolet Spark GT aparece primero por tener peso 3x en el campo `name` |

### TC-11: Autenticación JWT — request sin token

| Atributo | Valor |
|---|---|
| **ID** | TC-11 |
| **RF** | RNF-04 — Seguridad |
| **Descripción** | Invocar búsqueda sin enviar `Authorization` header |
| **Entrada** | `GET /api/search?q=Toyota` (sin header de auth) |
| **Resultado esperado** | Código `401 Unauthorized`. Kong rechaza la petición por falta de JWT |
| **Resultado obtenido** | ✅ `401 Unauthorized`. `{"message":"Unauthorized"}` |

### TC-12: Autenticación JWT — token inválido

| Atributo | Valor |
|---|---|
| **ID** | TC-12 |
| **RF** | RNF-04 — Seguridad |
| **Descripción** | Invocar búsqueda con un token mal formado |
| **Entrada** | `GET /api/search?q=Toyota` con `Authorization: Bearer token-invalido` |
| **Resultado esperado** | Código `401`. Kong no puede validar la firma |
| **Resultado obtenido** | ✅ `401 Unauthorized` |

### TC-13: Rate limiting — límite de peticiones

| Atributo | Valor |
|---|---|
| **ID** | TC-13 |
| **RF** | RNF-03 — Rendimiento |
| **Descripción** | Superar las 60 peticiones por minuto |
| **Entrada** | 61 requests consecutivos a `GET /api/search?q=Toyota` en menos de 1 minuto |
| **Resultado esperado** | Petición 61 devuelve `429 Too Many Requests` |
| **Resultado obtenido** | ✅ `429 Too Many Requests` con mensaje `API rate limit exceeded` |

### TC-14: Indexación por eventos — creación de producto (RF-08)

| Atributo | Valor |
|---|---|
| **ID** | TC-14 |
| **RF** | RF-08 — Indexación por eventos |
| **Descripción** | Crear un producto en Catalog API y verificar que aparezca en Search |
| **Entrada** | `POST /api/v1/catalog/products` con datos de un nuevo vehículo, luego `GET /api/search?q=MiNuevoAuto` |
| **Resultado esperado** | Código `201` en creación. Luego `200` en búsqueda con el nuevo producto en los resultados |
| **Resultado obtenido** | ✅ Producto creado, evento `product.created` publicado a RabbitMQ, Search lo consume, lo indexa en ES, y aparece en la búsqueda |

### TC-15: Indexación por eventos — actualización de producto (RF-08)

| Atributo | Valor |
|---|---|
| **ID** | TC-15 |
| **RF** | RF-08 — Indexación por eventos |
| **Descripción** | Actualizar el nombre de un producto existente y verificar que Search refleje el cambio |
| **Entrada** | `PUT /api/v1/catalog/products/{id}` con nuevo nombre, luego `GET /api/search?q=<nuevoNombre>` |
| **Resultado esperado** | La búsqueda devuelve el producto con el nombre actualizado |
| **Resultado obtenido** | ✅ Evento `product.updated` consumido por Search, documento reindexado en ES, nombre actualizado visible en búsqueda |

### TC-16: Cache — búsqueda repetitiva (RNF-03)

| Atributo | Valor |
|---|---|
| **ID** | TC-16 |
| **RF** | RNF-03 — Rendimiento |
| **Descripción** | Realizar la misma búsqueda dos veces seguidas. La segunda debe servirse desde Redis |
| **Entrada** | `GET /api/search?q=Toyota` dos veces |
| **Resultado esperado** | Ambas devuelven `200`. La segunda es más rápida (cache hit) y el resultado es idéntico |
| **Resultado obtenido** | ✅ Cache hit en segunda request. Tiempo de respuesta menor (medible vía headers o logs). Mismos resultados |

---

## 5. Resumen de resultados

| ID | Estado | Observaciones |
|---|---|---|
| TC-01 | ✅ | Búsqueda full-text funciona correctamente |
| TC-02 | ✅ | Validación de query vacía |
| TC-03 | ✅ | Validación de longitud máxima |
| TC-04 | ✅ | Fuzzy search — tolera 1 error de caracter |
| TC-05 | ✅ | Fuzzy search — tolera omisión de letra |
| TC-06 | ✅ | Lista vacía para términos inexistentes |
| TC-07 | ✅ | Autocompletado con prefijo |
| TC-08 | ✅ | Validación de query vacía en suggest |
| TC-09 | ✅ | Lista vacía para prefijo sin matching |
| TC-10 | ✅ | Ranking prioriza coincidencias en nombre |
| TC-11 | ✅ | Rechazo sin token JWT |
| TC-12 | ✅ | Rechazo con token inválido |
| TC-13 | ✅ | Rate limiting funciona (60 req/min) |
| TC-14 | ✅ | Indexación por evento de creación |
| TC-15 | ✅ | Indexación por evento de actualización |
| TC-16 | ✅ | Cache-aside funciona |

### Leyenda

- ✅ **Aprobado** — el resultado obtenido coincide con el esperado
- ❌ **Fallido** — el resultado obtenido difiere del esperado
- ⚠️ **No implementado** — el requerimiento no fue implementado en la versión actual

## 6. Hallazgos

1. **Filtros dinámicos (RF-02)**: No implementados. La API solo acepta `q`, `page` y `searchAfter`. No hay parámetros para filtrar por precio, categoría, disponibilidad ni rating.
2. **Ordenamiento (RF-03)**: No implementado. Los resultados se devuelven únicamente en orden de relevancia de Elasticsearch.
3. **Sugerencias relacionadas (RF-05)**: No implementado. El endpoint de suggest completa el término escrito pero no devuelve productos relacionados semánticamente.
4. **Paginación en backend (RF-09)**: No implementada. El controlador acepta `page` y `searchAfter` pero el repositorio no los utiliza; devuelve todos los resultados.

## 7. Comandos de ejecución (para reproducir)

```bash
# Obtener token
TOKEN=$(curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# TC-01: Búsqueda exitosa
curl -s "http://localhost:8000/api/search?q=Toyota" \
  -H "Authorization: Bearer $TOKEN" | jq .

# TC-04: Fuzzy search
curl -s "http://localhost:8000/api/search?q=Toyote" \
  -H "Authorization: Bearer $TOKEN" | jq .

# TC-07: Sugerencias
curl -s "http://localhost:8000/api/search/suggest?q=Toy" \
  -H "Authorization: Bearer $TOKEN" | jq .

# TC-11: Sin token (debe dar 401)
curl -s "http://localhost:8000/api/search?q=Toyota" | jq .

# TC-14: Carga de datos de demostración
curl -s -X POST http://localhost:8000/api/v1/catalog/products/bulk/demo \
  -H "Authorization: Bearer $TOKEN" | jq .
```
