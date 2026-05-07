# Fragmento de Informe - Implementacion de Cache Redis en Search Microservice

## 1. Contexto de la actividad

Durante esta actividad se implemento una capa de cache en Redis para el microservicio de busqueda, con el objetivo de reducir tiempos de respuesta en consultas repetidas y mantener consistencia eventual frente a cambios de producto procesados por eventos.

La solucion se integro sobre la arquitectura existente de Spring Boot, Elasticsearch y RabbitMQ, dejando preparado el flujo completo de:

- lectura cacheada para busquedas y sugerencias,
- expiracion automatica por TTL,
- e invalidacion de cache cuando se procesa un evento `ProductUpdated`.

## 2. Objetivo tecnico

El objetivo fue incorporar Redis como mecanismo de aceleracion para el read path de la API, de manera que:

- una consulta primero intente resolver desde cache,
- si no existe informacion cacheada consulte Elasticsearch,
- y luego almacene la respuesta temporalmente en Redis.

Adicionalmente, se implemento la invalidacion del cache despues de reindexar productos actualizados, con el fin de evitar respuestas obsoletas.

## 3. Componentes implementados

### 3.1 Repositorio de infraestructura para Redis

Se implemento la clase `RedisSearchCacheRepository` en la capa de infraestructura con las operaciones basicas:

- `get(key)`: recupera un valor almacenado en Redis.
- `set(key, value, ttl)`: guarda la respuesta serializada con tiempo de expiracion.
- `delete(key)`: elimina una entrada concreta del cache.

### 3.2 Estandarizacion de claves

Se definio una convencion uniforme para las llaves de Redis:

- Busquedas: `search:query:{hash}`
- Sugerencias: `search:suggest:{q}`

Esta estructura facilita la organizacion de datos y la invalidacion por patrones.

### 3.3 Configuracion de TTL

Se configuraron tiempos de vida diferenciados para balancear rendimiento y consistencia eventual:

- Busqueda general: `1 minuto`
- Sugerencias de autocompletado: `5 minutos`

### 3.4 Integracion del read path

Se incorporo un servicio de consulta cacheada que:

1. normaliza el termino de busqueda,
2. genera la llave correspondiente,
3. consulta primero en Redis,
4. y, en caso de no encontrar datos, consulta Elasticsearch y escribe el resultado en cache.

Los endpoints expuestos para la prueba fueron:

- `GET /api/search?q=...`
- `GET /api/search/suggest?q=...`

### 3.5 Integracion del write path

En el flujo reactivo del sistema, al consumir el evento `ProductUpdated` desde RabbitMQ:

1. el producto se reindexa en Elasticsearch,
2. luego se invalidan entradas relacionadas en Redis,
3. evitando que la API responda con informacion previa al cambio.

## 4. Archivos principales involucrados

Los archivos mas relevantes del trabajo realizado fueron:

- `src/main/java/infrastructure/redis/RedisSearchCacheRepository.java`
- `src/main/java/infrastructure/redis/SearchCacheKeyFactory.java`
- `src/main/java/infrastructure/search/CachedSearchService.java`
- `src/main/java/presentation/controllers/SearchController.java`
- `src/main/java/infrastructure/redis/RedisInvalidateCacheUseCase.java`
- `src/main/java/infrastructure/elasticsearch/ElasticsearchSearchRepository.java`
- `src/main/java/infrastructure/elasticsearch/IndexProductUseCaseImpl.java`
- `src/main/resources/application.yml`

## 5. Evidencias de ejecucion y validacion

En esta seccion se dejan los comandos utilizados y el espacio para insertar las capturas de pantalla de terminal correspondientes.

### 5.1 Estado de los contenedores

Comando sugerido:

```bash
docker compose -f deploy/docker-compose.yml ps
```

Resultado esperado:

- contenedor `search-api` levantado,
- Redis, RabbitMQ y Elasticsearch en estado saludable,
- puertos expuestos correctamente para pruebas locales.

**Captura sugerida:** salida donde se vea el estado `Up` o `healthy` de los servicios.

![Captura pendiente - estado de contenedores](./images/placeholder-contenedores.png)

### 5.2 Verificacion de salud de la API

Comando sugerido:

```bash
curl -i http://localhost:8085/actuator/health
```

Resultado esperado:

- codigo HTTP `200`,
- estado general `UP`,
- componentes `redis`, `rabbit` y `elasticsearch` reportados como disponibles.

**Captura sugerida:** salida completa del `curl` donde se observe el JSON de health.

![Captura pendiente - actuator health](./images/placeholder-health.png)

### 5.3 Prueba del endpoint de busqueda

Comando sugerido:

```bash
curl -i "http://localhost:8085/api/search?q=laptop"
```

Resultado esperado:

- codigo HTTP `200`,
- lista JSON con productos indexados que coincidan con el termino buscado.

**Captura sugerida:** salida donde aparezcan varios productos retornados por el endpoint.

![Captura pendiente - endpoint search](./images/placeholder-search.png)

### 5.4 Prueba del endpoint de sugerencias

Comando sugerido:

```bash
curl -i "http://localhost:8085/api/search/suggest?q=lap"
```

Resultado esperado:

- codigo HTTP `200`,
- arreglo JSON con sugerencias de nombres de productos.

**Captura sugerida:** salida mostrando las sugerencias generadas.

![Captura pendiente - endpoint suggest](./images/placeholder-suggest.png)

### 5.5 Verificacion de claves en Redis

Comando sugerido:

```bash
docker compose -f deploy/docker-compose.yml exec -T redis \
  redis-cli -a redis_secure_pass_123 --scan --pattern 'search:*'
```

Resultado esperado:

- aparicion de claves con prefijo `search:query:` y `search:suggest:`.

**Captura sugerida:** salida donde se observen las keys creadas por las consultas anteriores.

![Captura pendiente - redis keys](./images/placeholder-redis-keys.png)

### 5.6 Verificacion del TTL

Comando sugerido:

```bash
docker compose -f deploy/docker-compose.yml exec -T redis \
  redis-cli -a redis_secure_pass_123 TTL "search:suggest:lap"
```

Resultado esperado:

- valor positivo cercano a `300` segundos para sugerencias,
- valor positivo cercano a `60` segundos para una busqueda general.

**Captura sugerida:** salida mostrando el TTL de una key existente.

![Captura pendiente - redis ttl](./images/placeholder-redis-ttl.png)

### 5.7 Verificacion de logs del servicio

Comando sugerido:

```bash
docker compose -f deploy/docker-compose.yml logs --tail=100 search-api
```

Resultado esperado:

- inicializacion correcta del servicio,
- disponibilidad del indice en Elasticsearch,
- y, en escenarios de invalidacion, trazas relacionadas con la eliminacion de cache.

**Captura sugerida:** fragmento de logs donde se evidencie la operacion normal del servicio.

![Captura pendiente - logs search-api](./images/placeholder-logs.png)

## 6. Resultado obtenido

La implementacion permitio comprobar que el microservicio ya puede:

- responder consultas desde la API,
- almacenar respuestas temporales en Redis,
- reutilizar resultados cacheados,
- y borrar informacion cacheada cuando un producto cambia.

Con esto, la solucion queda alineada con una estrategia de consistencia eventual y mejora del tiempo de respuesta para consultas repetidas.

## 7. Observaciones

- La siembra de datos para pruebas puede realizarse localmente en Elasticsearch sin afectar el codigo fuente del proyecto.
- Las capturas recomendadas corresponden a evidencia de ejecucion en terminal, por lo que no requieren modificar el repositorio.
- Este fragmento puede integrarse directamente en el informe del sprint o anexarse como evidencia tecnica de la HU relacionada con Redis.
