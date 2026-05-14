# Security Implementation Summary

## Overview

Implementation of Rate Limiting, Input Sanitization, and Elasticsearch DSL Injection Protection for the Search Microservice.

## Files Modified

### 1. `pom.xml`
- Added `spring-boot-starter-validation` dependency to enable Jakarta Bean Validation (`@Validated`, `@NotBlank`, `@Size`).

### 2. `deploy/kong/kong.yml`
- Added two `rate-limiting` plugins to `search-service`:
  - **By Consumer** (JWT-based): `limit_by: consumer`, `minute: ${KONG_RATE_LIMIT_MINUTE:-60}`
  - **By IP**: `limit_by: ip`, `minute: ${KONG_RATE_LIMIT_MINUTE:-60}`
- Both use `policy: local` and `fault_tolerant: true`.
- Limits are configurable via the `KONG_RATE_LIMIT_MINUTE` environment variable (default: 60 rpm).

### 3. `src/main/java/presentation/controllers/SearchController.java`
- Added `@Validated` at class level and `@NotBlank` + `@Size(max=200)` annotations on the `q` parameter in both `search()` and `suggest()` endpoints.
- Integrated `EsQuerySanitizer` — user queries are sanitized before being passed to `CachedSearchService`.
- Added programmatic length validation (≤ 200 chars) for defense-in-depth.
- Updated constructor to receive `EsQuerySanitizer`.

### 4. `src/main/java/presentation/advice/GlobalExceptionHandler.java`
- Added handler for `ConstraintViolationException` → 400 Bad Request (for `@Validated` method-level validation).
- Added handler for `MethodArgumentNotValidException` → 400 Bad Request (for `@Valid` on request bodies).
- Added handler for `RateLimitExceededException` → 429 Too Many Requests.
- All handlers return `{"error": "<message>"}` JSON format.

### 5. `src/main/java/presentation/advice/RateLimitExceededException.java` (NEW)
- Custom `RuntimeException` for rate limit violations, mapped to HTTP 429.

### 6. `src/main/java/infrastructure/elasticsearch/EsQuerySanitizer.java` (NEW)
- `@Component` utility that strips ES-special characters from user queries.
- Characters removed: `* ? ~ ^ { } [ ] ( ) : \ / " + - = > < ! & |`
- Normalizes whitespace and trims result.
- Applied in `SearchController` before queries reach `CachedSearchService` and Elasticsearch.

### 7. `src/main/java/infrastructure/security/RateLimitInterceptor.java` (NEW)
- Spring `HandlerInterceptor` providing defense-in-depth rate limiting.
- Sliding window counter per client (1-minute window).
- Identifies clients via `X-Forwarded-For` header (set by Kong), falls back to `RemoteAddr`.
- Configured via `search.security.rate-limit.requests-per-minute` (default: 60).
- Conditionally enabled via `search.security.rate-limit.enabled` (default: `true`).
- Throws `RateLimitExceededException` when exceeded, resulting in HTTP 429.

### 8. `src/main/java/infrastructure/security/RateLimitWebConfig.java` (NEW)
- Registers `RateLimitInterceptor` for `/api/search` and `/api/search/suggest` paths.
- Conditional on the interceptor bean being available.

### 9. `src/main/resources/application.yml`
- Added `search.security.rate-limit` property group:
  - `enabled`: `${SEARCH_RATE_LIMIT_ENABLED:true}`
  - `requests-per-minute`: `${SEARCH_RATE_LIMIT_RPM:60}`

### 10. `src/test/java/infrastructure/elasticsearch/EsQuerySanitizerTest.java` (NEW)
- 14 unit tests verifying sanitizer behavior:
  - Removes wildcards (`*`, `?`), boost/fuzziness (`~`, `^`), JSON delimiters (`{}`, `[]`, `""`), query operators (`+`, `-`, `=`, `&`, `|`, `!`, `>`, `<`), colons, slashes, backslashes.
  - Normalizes whitespace and trims.
  - Preserves alphanumeric, spaces, and Spanish characters.
  - Handles null input and all-special-char input.

### 11. `src/test/java/infrastructure/security/RateLimitInterceptorTest.java` (NEW)
- 4 unit tests for the rate limiter:
  - Allows requests under the limit.
  - Blocks requests over the limit with correct exception message.
  - Uses `X-Forwarded-For` header for client identification.
  - Tracks different clients independently.

### 12. `src/test/java/presentation/controllers/SearchControllerSecurityTest.java` (NEW)
- 9 integration-style security tests:
  - **Size tests**: 201-char `q` returns 400 on both `/search` and `/suggest`.
  - **Boundary tests**: 200-char `q` returns 200 (accepted).
  - **Injection/sanitization tests**: Malicious DSL payloads (`{"bool":...}`, `*`, `name:admin OR 1=1`, `test~^boost*`, `laptop^999`) are sanitized before reaching `CachedSearchService`.
  - Uses `standaloneSetup` with real `EsQuerySanitizer` for integration coverage.

### 13. `src/test/java/presentation/controllers/SearchControllerTest.java`
- Updated constructor call to pass `EsQuerySanitizer` mock (required by updated controller).
- All 5 existing tests continue to pass.

### 14. `src/test/java/infrastructure/elasticsearch/IndexProductUseCaseImplTest.java`
- Fixed pre-existing compilation error: added `MeterRegistry` parameter to constructor call (required by `IndexProductUseCaseImpl`).
- Uses `SimpleMeterRegistry` for tests.

### 15. `README.md`
- Added **Security Policies** section documenting:
  - Rate limiting configuration (Kong + Spring).
  - Input validation constraints.
  - ES DSL injection protection strategy.
  - Error response format table (400, 429, 500).

## Repository Audit: ElasticsearchSearchRepository

**No changes needed.** The repository already uses `co.elastic.clients.elasticsearch.ElasticsearchClient` with typed DSL builders (`multi_match` for search, `match_phrase_prefix` for suggestions). User input is passed as a field value within structured query objects — never concatenated into raw JSON query strings. The `EsQuerySanitizer` provides defense-in-depth by stripping special characters before queries reach the repository.

## Test Results

```
Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 85 tests pass, including:
- 14 existing tests (unchanged logic)
- 14 `EsQuerySanitizer` unit tests
- 9 `SearchControllerSecurity` tests (size, injection, boundary)
- 4 `RateLimitInterceptor` unit tests
- 1 test fix (`IndexProductUseCaseImplTest`)

## Security Layers Summary

| Layer | Mechanism | Response |
|-------|-----------|----------|
| Kong Gateway | `rate-limiting` plugin (consumer + IP) | 429 |
| Spring Boot | `RateLimitInterceptor` (defense-in-depth) | 429 |
| Bean Validation | `@NotBlank`, `@Size(max=200)` on `q` param | 400 |
| Programmatic Check | `validateQuery()` with blank + length checks | 400 |
| ES Sanitization | `EsQuerySanitizer` strips special characters | Transforms input |
| ES Client Safety | Typed DSL builders (`multi_match`, `match_phrase_prefix`) | Inherently safe |
