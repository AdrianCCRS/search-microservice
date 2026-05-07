# Plan de Implementacion: Invalidacion de Cache Redis

## Objetivo

Implementar la invalidacion de cache solicitada en `cache-invalidation-instructions.md` para eventos `ProductUpdated` y `ProductCreated`, siguiendo buenas practicas Redis de rendimiento, seguridad operativa y observabilidad.

El cambio debe evitar comandos bloqueantes como `KEYS *`, usar `SCAN` para descubrir claves por patron y `UNLINK` para borrar de forma no bloqueante.

## Contexto del Repositorio

- La clase mencionada en las instrucciones como `InvalidateCacheUseCaseImpl` corresponde en este proyecto a `infrastructure.redis.RedisInvalidateCacheUseCase`.
- El repositorio de cache actual es `infrastructure.redis.RedisSearchCacheRepository`.
- Las claves actuales siguen estos formatos:
- `search:query:{sha256(normalizedQuery)}` para resultados de busqueda.
- `search:suggest:{normalizedQuery}` para sugerencias.
- La normalizacion actual esta centralizada en `SearchCacheKeyFactory.normalize(...)`: trim, colapso de espacios y lowercase.
- Los consumidores actuales son `ProductUpdatedConsumer` y `ProductCreatedConsumer`.

## Reglas Redis Aplicadas

- `data-key-naming`: conservar prefijos consistentes `search:query:` y `search:suggest:`.
- `ram-ttl`: mantener TTL en escrituras de cache; la invalidacion complementa el TTL, no lo reemplaza.
- `conn-blocking`: no usar `KEYS`; usar `SCAN` con `MATCH` y `COUNT` configurable.
- `conn-pipelining`: borrar claves por lotes cuando sea posible para reducir round trips.
- `conn-pooling`: reutilizar `StringRedisTemplate` y la configuracion de conexiones existente de Spring.
- `conn-timeouts`: no introducir conexiones manuales sin timeouts; delegar en la configuracion Spring Boot/Redis existente.
- `observe-commands`: registrar la cantidad de claves eliminadas y el evento que disparo la invalidacion.
- `security-auth`: no cambiar credenciales ni conexion; respetar la configuracion actual de Redis protegida por password en compose.

## Alcance Funcional

### Evento ProductUpdated

Al recibir `ProductUpdated` se debe:

1. Reindexar el producto en Elasticsearch como ya ocurre actualmente.
2. Invalidar todas las busquedas cacheadas:

```text
search:query:*
```

3. Invalidar las sugerencias relacionadas con el prefijo del nombre anterior:

```text
search:suggest:{prefix}*
```

4. Registrar:

```text
Invalidadas {} claves de cache por evento {}
```

### Evento ProductCreated

Al recibir `ProductCreated` se debe:

1. Indexar el nuevo producto en Elasticsearch como ya ocurre actualmente.
2. Invalidar sugerencias para permitir que el nuevo producto aparezca:

```text
search:suggest:*
```

3. Registrar:

```text
Invalidadas {} claves de cache por evento {}
```

## Decision sobre Nombre Anterior

Las instrucciones piden invalidar `search:suggest:*` que contenga el prefijo del nombre anterior en `ProductUpdated`.

El contrato actual `ProductData` solo contiene `name`, no contiene `previousName`, `oldName` ni un evento especifico `ProductUpdatedEvent` con datos previos.

Plan recomendado:

1. Implementar la invalidacion usando `event.data().name()` como prefijo disponible actualmente.
2. Mantener el diseno preparado para cambiar a `previousName` cuando el evento de Catalog lo incluya.
3. Si el contrato de eventos puede modificarse ahora, crear un `ProductUpdatedEvent` con `previousName` y usar ese campo como fuente correcta.

## Cambios de Codigo Propuestos

### 1. Actualizar `InvalidateCacheUseCase`

Archivo:

```text
src/main/java/application/usecases/InvalidateCacheUseCase.java
```

Cambiar el contrato actual:

```java
void execute(String productId);
```

Por metodos orientados a eventos:

```java
void invalidateProductUpdated(ProductData data);
void invalidateProductCreated(ProductData data);
```

Motivo:

- La invalidacion ya no depende solo de `productId`.
- `ProductUpdated` y `ProductCreated` tienen reglas distintas.
- El caso de uso queda alineado con eventos de dominio/aplicacion.

### 2. Implementar `deleteByPattern` en `RedisSearchCacheRepository`

Archivo:

```text
src/main/java/infrastructure/redis/RedisSearchCacheRepository.java
```

Agregar metodo:

```java
long deleteByPattern(String pattern);
```

Comportamiento:

- Validar que `pattern` no sea nulo ni vacio.
- Ejecutar `SCAN` con `MATCH pattern` y `COUNT` configurable.
- Acumular claves por lotes razonables.
- Ejecutar `UNLINK` para borrado asincrono/no bloqueante.
- Retornar el total de claves enviadas a borrado.

Pseudocodigo:

```java
public long deleteByPattern(String pattern) {
    validateKey(pattern);

    ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(scanCount)
            .build();

    long deleted = 0;
    List<String> batch = new ArrayList<>(unlinkBatchSize);

    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            batch.add(cursor.next());
            if (batch.size() >= unlinkBatchSize) {
                deleted += unlink(batch);
                batch.clear();
            }
        }
    }

    if (!batch.isEmpty()) {
        deleted += unlink(batch);
    }

    return deleted;
}
```

Notas de implementacion:

- Preferir `redisTemplate.unlink(Collection<String>)` si esta disponible en la version de Spring Data Redis usada.
- Si no esta disponible, usar `redisTemplate.execute(...)` para emitir `UNLINK` de forma explicita.
- No reemplazar esto por `redisTemplate.keys(pattern)`.

### 3. Refactorizar `RedisInvalidateCacheUseCase`

Archivo:

```text
src/main/java/infrastructure/redis/RedisInvalidateCacheUseCase.java
```

Responsabilidades nuevas:

- Orquestar que patrones invalidar por evento.
- Delegar el escaneo y borrado en `RedisSearchCacheRepository.deleteByPattern(...)`.
- Normalizar prefijos de sugerencias con `SearchCacheKeyFactory`.
- Sumar el total de claves invalidadas.
- Emitir el log obligatorio.

Patrones:

```java
private static final String PRODUCT_UPDATED_EVENT = "ProductUpdated";
private static final String PRODUCT_CREATED_EVENT = "ProductCreated";
```

Para `ProductUpdated`:

```java
long deleted = 0;
deleted += cacheRepository.deleteByPattern("search:query:*");
deleted += cacheRepository.deleteByPattern("search:suggest:" + normalizedNamePrefix + "*");
log.info("Invalidadas {} claves de cache por evento {}", deleted, PRODUCT_UPDATED_EVENT);
```

Para `ProductCreated`:

```java
long deleted = cacheRepository.deleteByPattern("search:suggest:*");
log.info("Invalidadas {} claves de cache por evento {}", deleted, PRODUCT_CREATED_EVENT);
```

Validaciones:

- Rechazar `ProductData` nulo.
- Rechazar `productId` vacio solo si se sigue usando para mensajes o validaciones del evento.
- Para nombre nulo/vacio en `ProductUpdated`, invalidar al menos `search:query:*` y omitir el patron de sugerencia especifico, o usar `search:suggest:*` como fallback conservador.

Fallback recomendado:

- Si `ProductUpdated.data().name()` esta vacio, invalidar `search:suggest:*` para evitar sugerencias obsoletas.

### 4. Conectar `ProductUpdatedConsumer`

Archivo:

```text
src/main/java/infrastructure/messaging/consumers/ProductUpdatedConsumer.java
```

Cambiar:

```java
invalidateCacheUseCase.execute(event.data().productId());
```

Por:

```java
invalidateCacheUseCase.invalidateProductUpdated(event.data());
```

Mantener orden:

1. Reindexar producto.
2. Invalidar cache.
3. Confirmar mensaje con `basicAck`.

Si falla indexacion o invalidacion, mantener `basicNack(tag, false, false)` para DLQ.

### 5. Conectar `ProductCreatedConsumer`

Archivo:

```text
src/main/java/infrastructure/messaging/consumers/ProductCreatedConsumer.java
```

Agregar dependencia:

```java
private final InvalidateCacheUseCase invalidateCacheUseCase;
```

Despues de indexar:

```java
indexProductUseCase.execute(event.data());
invalidateCacheUseCase.invalidateProductCreated(event.data());
```

Mantener `basicAck` solo cuando ambas operaciones finalicen correctamente.

## Configuracion Propuesta

Agregar o reutilizar propiedades:

```properties
search.cache.redis.query-key-pattern=search:query:*
search.cache.redis.suggest-key-pattern=search:suggest:*
search.cache.redis.scan-count=1000
search.cache.redis.unlink-batch-size=500
```

Razon:

- `scan-count` permite ajustar el costo de cada iteracion `SCAN`.
- `unlink-batch-size` evita acumular demasiadas claves en memoria y reduce round trips.

## Plan de Pruebas Unitarias

### `RedisSearchCacheRepositoryTest`

Agregar pruebas para:

- `deleteByPattern` rechaza patron nulo o vacio.
- `deleteByPattern("search:query:*")` usa `SCAN` con patron.
- `deleteByPattern` usa `UNLINK` o el metodo equivalente de Spring Data Redis, no `delete` por clave.
- `deleteByPattern` retorna la cantidad de claves encontradas/enviadas a borrado.
- `deleteByPattern` retorna `0` cuando `SCAN` no encuentra claves.

### `RedisInvalidateCacheUseCaseTest`

Actualizar pruebas para:

- `invalidateProductUpdated(data)` llama `deleteByPattern("search:query:*")`.
- `invalidateProductUpdated(data)` llama `deleteByPattern("search:suggest:{nombre-normalizado}*")`.
- `invalidateProductCreated(data)` llama `deleteByPattern("search:suggest:*")`.
- El total de claves invalidado se calcula sumando los retornos del repositorio.
- Datos invalidos lanzan `IllegalArgumentException` o aplican fallback definido.

### `ProductUpdatedConsumerTest`

Actualizar verificacion:

```java
verify(invalidateCacheUseCase).invalidateProductUpdated(data);
```

### `ProductCreatedConsumerTest`

Agregar mock de `InvalidateCacheUseCase` y verificar:

```java
verify(invalidateCacheUseCase).invalidateProductCreated(data);
```

Agregar caso de error:

- Si falla invalidacion, el consumidor debe ejecutar `basicNack(tag, false, false)`.

## Validacion Manual

### Busqueda

1. Levantar entorno:

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

2. Ejecutar busqueda:

```http
GET /api/search?q=laptop
```

3. Confirmar en Redis que existe una clave:

```text
search:query:{hash}
```

4. Publicar evento `ProductUpdated` para la laptop.
5. Ejecutar nuevamente:

```http
GET /api/search?q=laptop
```

6. Confirmar que la respuesta se obtiene fresca desde Elasticsearch y que las claves `search:query:*` fueron eliminadas.

### Sugerencias

1. Ejecutar sugerencia:

```http
GET /api/search/suggest?q=Lap
```

2. Confirmar en Redis:

```text
search:suggest:lap
```

3. Publicar evento `ProductCreated` con nombre `Laptop Gaming X`.
4. Ejecutar nuevamente:

```http
GET /api/search/suggest?q=Lap
```

5. Confirmar que `Laptop Gaming X` aparece en las sugerencias.

## Criterios de Aceptacion

- `ProductUpdated` invalida `search:query:*`.
- `ProductUpdated` invalida sugerencias relacionadas con el prefijo disponible del nombre.
- `ProductCreated` invalida `search:suggest:*`.
- No se usa `KEYS` en ningun punto de la implementacion.
- El borrado usa `UNLINK` o comando equivalente no bloqueante.
- Existe el metodo `deleteByPattern(String pattern)` en `RedisSearchCacheRepository`.
- `RedisInvalidateCacheUseCase` esta conectado al repositorio y no implementa directamente el borrado por patron.
- Se registra el log `Invalidadas {} claves de cache por evento {}`.
- `mvn test` pasa correctamente.

## Riesgos y Mitigaciones

- Riesgo: `ProductUpdated` no incluye nombre anterior.
- Mitigacion: usar `name` actual como prefijo disponible y documentar que debe migrarse a `previousName` cuando Catalog lo publique.

- Riesgo: invalidar `search:suggest:*` en `ProductCreated` puede borrar muchas claves.
- Mitigacion: usar `SCAN` + `UNLINK` por lotes y conservar TTL para limitar crecimiento.

- Riesgo: patron muy amplio en entornos con muchas claves.
- Mitigacion: mantener prefijos especificos `search:query:` y `search:suggest:`; nunca usar `*` global.

- Riesgo: falla Redis durante consumo de evento.
- Mitigacion: mantener `basicNack(tag, false, false)` para enviar a DLQ y evitar confirmar eventos no procesados completamente.
