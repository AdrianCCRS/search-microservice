# Cache Invalidation Summary

## What Was Implemented

Redis cache invalidation was implemented for product catalog events consumed by the Search Microservice.

The implementation covers:

- `ProductUpdated` events.
- `ProductCreated` events.
- Pattern-based Redis cache deletion.
- Non-blocking key deletion using `UNLINK`.
- Safe key discovery using `SCAN` instead of `KEYS`.
- Unit tests for repository, use case, and RabbitMQ consumers.
- Required observability log message:

```text
Invalidadas {} claves de cache por evento {}
```

## Files Involved

Main implementation:

- `src/main/java/application/usecases/InvalidateCacheUseCase.java`
- `src/main/java/infrastructure/redis/RedisInvalidateCacheUseCase.java`
- `src/main/java/infrastructure/redis/RedisSearchCacheRepository.java`
- `src/main/java/infrastructure/messaging/consumers/ProductUpdatedConsumer.java`
- `src/main/java/infrastructure/messaging/consumers/ProductCreatedConsumer.java`

Tests:

- `src/test/java/infrastructure/redis/RedisSearchCacheRepositoryTest.java`
- `src/test/java/infrastructure/redis/RedisInvalidateCacheUseCaseTest.java`
- `src/test/java/infrastructure/messaging/consumers/ProductUpdatedConsumerTest.java`
- `src/test/java/infrastructure/messaging/consumers/ProductCreatedConsumerTest.java`

## How It Works

### ProductUpdated

When a `ProductUpdated` event is consumed:

1. The product is reindexed in Elasticsearch.
2. All cached search query results are invalidated:

```text
search:query:*
```

3. Suggestion cache entries related to the product name are invalidated using the normalized product name:

```text
search:suggest:{normalized-product-name}*
```

Example:

```text
Product name: "  Gaming   Laptop  "
Invalidated pattern: search:suggest:gaming laptop*
```

4. If the product name is null or blank, the implementation falls back to invalidating all suggestion cache keys:

```text
search:suggest:*
```

5. The RabbitMQ message is acknowledged only after indexing and cache invalidation both succeed.
6. If indexing or cache invalidation fails, the message is nacked and sent to the DLQ.

### ProductCreated

When a `ProductCreated` event is consumed:

1. The product is indexed in Elasticsearch.
2. All suggestion cache entries are invalidated so the new product can appear in suggestions:

```text
search:suggest:*
```

3. The RabbitMQ message is acknowledged only after indexing and cache invalidation both succeed.
4. If indexing or cache invalidation fails, the message is nacked and sent to the DLQ.

## Redis Strategy

The repository method `deleteByPattern(String pattern)` performs deletion safely:

1. Validates that the pattern is not null, empty, or blank.
2. Uses Redis `SCAN` with `MATCH` to discover keys by pattern.
3. Collects keys in configurable batches.
4. Deletes keys using `UNLINK`, which is non-blocking compared with `DEL`.
5. Returns the number of keys submitted for deletion.

Important Redis behavior:

- `KEYS *` is not used.
- `UNLINK` is used instead of per-key blocking deletes.
- Batch size and scan count are configurable.

## Configuration

The implementation supports these Redis cache properties:

```properties
search.cache.redis.query-key-pattern=search:query:*
search.cache.redis.suggest-key-pattern=search:suggest:*
search.cache.redis.scan-count=1000
search.cache.redis.unlink-batch-size=500
```

Defaults are already provided in code through `@Value` annotations.

## Tests Added Or Updated

### RedisSearchCacheRepositoryTest

Verifies that:

- `deleteByPattern` rejects null, empty, and blank patterns.
- `deleteByPattern("search:query:*")` uses `SCAN` with the expected pattern.
- Matching keys are deleted with `UNLINK`.
- Per-key `delete` is not used for pattern deletion.
- Keys are unlinked in batches.
- The returned count matches the number of keys submitted for deletion.
- Zero is returned when no keys match.

### RedisInvalidateCacheUseCaseTest

Verifies that:

- `invalidateProductUpdated(data)` deletes `search:query:*`.
- `invalidateProductUpdated(data)` deletes `search:suggest:{normalized-name}*`.
- Blank or null names fall back to `search:suggest:*`.
- `invalidateProductCreated(data)` deletes `search:suggest:*`.
- The log message includes the total invalidated keys and event name.
- Null `ProductData` is rejected.

### ProductUpdatedConsumerTest

Verifies that:

- The product is reindexed.
- `invalidateProductUpdated(data)` is called.
- The message is acked on success.
- The message is nacked when invalidation fails.
- The message is not acked after an invalidation failure.

### ProductCreatedConsumerTest

Verifies that:

- The product is indexed.
- `invalidateProductCreated(data)` is called.
- The message is acked on success.
- The message is nacked when indexing fails.
- The message is nacked when invalidation fails.
- The message is not acked after a failure.

## How To Test

Run the targeted cache invalidation tests:

```bash
mvn -Dtest=RedisSearchCacheRepositoryTest,RedisInvalidateCacheUseCaseTest,ProductUpdatedConsumerTest,ProductCreatedConsumerTest test
```

If Maven is not installed locally, run them through Docker:

```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn -Dtest=RedisSearchCacheRepositoryTest,RedisInvalidateCacheUseCaseTest,ProductUpdatedConsumerTest,ProductCreatedConsumerTest test
```

Run the full test suite:

```bash
mvn test
```

Or with Docker:

```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn test
```

## Validation Result

The full Maven test suite was executed through Docker and passed:

```text
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Manual Validation Ideas

### Search Cache

1. Start the local environment:

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

2. Call the search endpoint:

```http
GET /api/search?q=laptop
```

3. Confirm a Redis key exists with prefix:

```text
search:query:
```

4. Publish a `ProductUpdated` event.
5. Confirm matching `search:query:*` keys are invalidated.

### Suggestion Cache

1. Call the suggest endpoint:

```http
GET /api/search/suggest?q=Lap
```

2. Confirm a Redis key exists:

```text
search:suggest:lap
```

3. Publish a `ProductCreated` event.
4. Confirm `search:suggest:*` keys are invalidated.
5. Call the suggest endpoint again and confirm fresh suggestions are returned.

## Acceptance Criteria Covered

- `ProductUpdated` invalidates `search:query:*`.
- `ProductUpdated` invalidates related suggestion cache by normalized product name.
- `ProductUpdated` falls back to `search:suggest:*` when the name is missing.
- `ProductCreated` invalidates `search:suggest:*`.
- Redis `KEYS` is not used.
- Redis `SCAN` is used for key discovery.
- Redis `UNLINK` is used for non-blocking deletion.
- Cache invalidation is delegated to `RedisSearchCacheRepository.deleteByPattern(...)`.
- Consumers only ack after indexing and invalidation succeed.
- Consumers nack failed processing to DLQ.
- Unit tests pass.
