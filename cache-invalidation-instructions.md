# Tarea Asignada: Completar Invalidación de Caché

[cite_start]**Responsable:** Yeison Caceres [cite: 158]

## [cite_start]1. Lógica de Invalidación (Capa de Aplicación) [cite: 72]
* [cite_start]Extender la funcionalidad de `InvalidateCacheUseCaseImpl`[cite: 73].
* [cite_start]Al recibir el evento `ProductUpdated`, se debe invalidar `search:query:*`, eliminando todas las búsquedas cacheadas[cite: 74, 75, 158].
* [cite_start]Al recibir el evento `ProductUpdated`, también se debe invalidar `search:suggest:*` que contenga el prefijo del nombre anterior[cite: 76].
* [cite_start]Al recibir el evento `ProductCreated`, se debe invalidar `search:suggest:*` para permitir que el nuevo producto aparezca en las sugerencias[cite: 77, 78, 158].
* [cite_start]Es obligatorio utilizar `SCAN` con un patrón en lugar de usar `KEYS *` para evitar bloquear Redis[cite: 79].
* [cite_start]Se debe agregar el siguiente log: "Invalidadas {} claves de caché por evento {}"[cite: 80].

## [cite_start]2. Implementación en Repositorio (Capa de Infraestructura) [cite: 81]
* [cite_start]Agregar la definición del método `deleteByPattern(String pattern)` en la interfaz/clase `RedisSearchCacheRepository`[cite: 82, 158].
* [cite_start]Implementar el método utilizando `SCAN` y `UNLINK` para realizar un borrado no bloqueante[cite: 83, 158].
* [cite_start]Asegurar la conexión de este método con `InvalidateCacheUseCaseImpl`[cite: 84].

## [cite_start]3. Pruebas y Validación [cite: 85, 158]
* **Validación de Búsqueda:**
    * [cite_start]Realizar la búsqueda "laptop", lo cual debe generar un resultado cacheado en `search:query:{hash}`[cite: 86].
    * [cite_start]Publicar el evento `ProductUpdated` (por ejemplo, alterando el precio de la laptop)[cite: 87].
    * [cite_start]Buscar "laptop" nuevamente para confirmar que se obtiene un resultado fresco desde Elasticsearch y no el caché anterior[cite: 88, 158].
    * [cite_start]Verificar utilizando Redis CLI que las claves de caché fueron eliminadas exitosamente[cite: 89].
* **Validación de Sugerencias:**
    * [cite_start]Buscar la sugerencia "Lap", asegurando que se cachee en `search:suggest:lap`[cite: 90, 91].
    * [cite_start]Publicar el evento `ProductCreated` para un ítem con el nombre "Laptop Gaming X"[cite: 92].
    * [cite_start]Buscar la sugerencia "Lap" nuevamente y verificar que el nuevo producto aparece en la lista[cite: 93].