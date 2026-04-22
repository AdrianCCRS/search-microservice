# Soluciones Implementadas - Sprint 1 (Search Microservice)

Este documento detalla las soluciones técnicas y configuraciones aplicadas para las tareas correspondientes a la infraestructura base y mensajería del Search Microservice, desarrollado inicialmente para procesar eventos mediante el patrón CQRS y la Clean Architecture empleando Java / Spring Boot.

---

## Story: Infraestructura base

### 1. Configurar Docker Compose base
Se inicializó el archivo `deploy/docker-compose.yml` que actúa como orquestador local.
- **Cómo se hizo**:
  - Se definieron estructuras genéricas para una red común `search-network` (tipo bridge).
  - Se preestableció un contenedor comentado para la API de Spring Boot (`search-api`), con sus respectivas variables de entorno para inyectar configuraciones dinámicamente (`SPRING_PROFILES_ACTIVE`, URLs, credenciales). 

### 2. Configurar Redis
Redis será el encargado de funcionar como caché de contingencia y acelerador de consultas para endpoints repetitivos.
- **Cómo se hizo**:
  - En el `docker-compose.yml`, se implementó el contenedor usando la imagen `redis:7.0-alpine`.
  - Se mapeó un volumen persistente (`redis_data`) en la carpeta `/data`.
  - Se configuró la seguridad empleando el flag de inicialización `--requirepass` que previene el acceso al CLI sin autenticación. Las credenciales fueron movidas a un archivo oculto local `.env`.
  - Se implementó un `healthcheck` usando el comando `redis-cli ping` devolviendo `PONG` para confirmar que el servicio se encuentra vivo.

### 3. Configurar Elasticsearch
Elasticsearch constituye el repositorio central optimizado para búsquedas.
- **Cómo se hizo**:
  - Configurado en modo `single-node` utilizando la imagen `docker.elastic.co/elasticsearch/elasticsearch:8.11.3`.
  - Se inhabilitó la seguridad de x-pack (`xpack.security.enabled=false`) para facilitar el desarrollo local y se limitó la carga en JVM a 512mb (`ES_JAVA_OPTS`).
  - El healthcheck de ES realiza un `curl` contra el puerto `9200` y cuenta con un periodo de reserva o de compensación de arranque (`start_period: 30s`), sabiendo que la base de datos es lenta al levantar al basarse en Java.
  - Para el Search Model, se elaboró el documento en `deploy/elasticsearch/mappings/search_document.json` definiendo los campos indexables clave como `category` (tipo keyword), `price` y `description`. Esto mismo se emparejó con la entidad `SearchDocument.java` en la capa de `Domain`.

---

## Story: Mensajería (RabbitMQ)

### 1. Configurar RabbitMQ (colas + exchange)
RabbitMQ canaliza todos los eventos asíncronos provenientes del microservicio Catalog siendo este el encolador AMQP por excelencia de los mensajes emitidos.
- **Cómo se hizo**:
  - En Docker: se instaló la versión `rabbitmq:3.12-management-alpine` configurando acceso al puerto de gestión web `15672` e inyectando un robusto de pruebas locales de conexión nativos en erlang (`healthcheck: rabbitmq-diagnostics ping`).
  - En Configuración: Se inyectaron los archivos `definitions.json` y `rabbitmq.conf` mapeándolos directamente como volúmenes hacia la ruta interior `/etc/rabbitmq/` en el contenedor. A su vez, se añadió la etiqueta `:z` a la directiva de Docker para sobrellevar la protección de Host que el módulo estricto "SELinux" ejerce sobre plataformas RedHat limitando lectura remota.
  - El `.json` establece un exchange (`catalog.events`), dos colas primarias (`search.product.created`, `search.product.updated`) y asocia por default un paso de enrutado fallido de escape o *Dead Letter fallback* (`x-dead-letter-exchange`).

### 2. Implementar consumer ProductCreated
Un servicio atado al listener AMQP que extrae datos nuevos de creados recientemente.
- **Cómo se hizo**:
  - Se originó el archivo `pom.xml` con las dependencias iniciales como `spring-boot-starter-amqp`.
  - Se creó y documentó el modelo del Contrato del Evento proveniente mediante Records de java en la capa de `Application` (`ProductCreatedEvent.java` y `ProductData.java`).
  - Se configuró a nivel interno `@Bean` la cola por pre-carga serializando al conversor `Jackson2JsonMessageConverter` y se forzó en factory un modo `AcknowledgeMode.MANUAL`.
  - Se implementó la clase `@Component` llamada `ProductCreatedConsumer` en Infrastructure. Ella ejecuta el servicio `indexProductUseCase` inyectado por interfaz y devuelve un `.basicAck`. También captura errores para enviarlos a la Dead Letter forzando un log con el Fallo y un `.basicNack` pasándole la etiqueta respectiva del evento original.

### 3. Implementar consumer ProductUpdated
Maneja la lógica simultánea de indexación y eliminación de caché en caso de una alteración a nivel del Catalog.
- **Cómo se hizo**:
  - Se elaboró la implementación en el componente `ProductUpdatedConsumer`. Consumiendo directamente la misma payload sobre la cola de colas en un Thread alternativo.
  - Aparte del proceso repetido genérico de reconstruir para Elastic, introduce al dummy interno `InvalidateCacheUseCase.execute(productId)`, emulando la alerta encargada de mandar la orden para limar del caché `Redis` obsoletiado el componente.
  - Se aplicó cobertura de redacción implementando tests unitarios por Mockito que validan y certifican la existencia y validacion de un `Verify` en los flujos completos con ACK o validación paralela al Exception con el control estricto que lo manda al `.basicNack()`.

### 4. Manejo de errores y reintentos (Dead Letter)
El mecanismo principal de protección frente a indisponibilidades en la base.
- **Cómo se hizo**:
  - Implementación a nivel del recurso en el fichero `application.yml` introduciendo el bloque "Retry" natural con las propiedades: `max-attempts=3` e `initial-interval=5000` con `multiplier=3`. De esta maneja el backoff realiza las peticiones entre los tiempos configurados: 5s → 15s → 45s.
  - Una superada la terna el fallback direcciona lo rechazado permanentemente a la Dead Letter automáticamente. Se generó entonces el componente independiente de lectura extra `DeadLetterConsumer.java` anotado para procesar esta DLQ (`search.dead.letter`), que de ser contactada escribe en formato `WARN e ERROR` los headers con los problemas y el contenido *Body* rescatable al LOG interno del sistema percatado.
  - Se inyectó además la dependencia `spring-boot-starter-actuator` y se actualizó el properties encendiendo los endpoints nativos en la capa web (`management.endpoints.web.exposure.include="health"`).
