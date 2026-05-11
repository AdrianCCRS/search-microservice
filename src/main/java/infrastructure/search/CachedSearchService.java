package infrastructure.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entities.SearchDocument;
import infrastructure.elasticsearch.ElasticsearchSearchRepository;
import infrastructure.redis.RedisSearchCacheRepository;
import infrastructure.redis.SearchCacheKeyFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CachedSearchService {

    private static final TypeReference<List<SearchDocument>> SEARCH_RESULT_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> SUGGESTION_RESULT_TYPE = new TypeReference<>() {};

    private final ElasticsearchSearchRepository elasticsearchSearchRepository;
    private final RedisSearchCacheRepository cacheRepository;
    private final SearchCacheKeyFactory keyFactory;
    private final ObjectMapper objectMapper;
    private final Duration searchTtl;
    private final Duration suggestTtl;

    // --- Micrometer metrics ---
    private final Counter searchCacheHits;
    private final Counter searchCacheMisses;
    private final Counter suggestCacheHits;
    private final Counter suggestCacheMisses;
    private final Timer searchEsTimer;
    private final Timer suggestEsTimer;

    public CachedSearchService(
            ElasticsearchSearchRepository elasticsearchSearchRepository,
            RedisSearchCacheRepository cacheRepository,
            SearchCacheKeyFactory keyFactory,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${search.cache.redis.search-ttl:1m}") Duration searchTtl,
            @Value("${search.cache.redis.suggest-ttl:5m}") Duration suggestTtl) {
        this.elasticsearchSearchRepository = elasticsearchSearchRepository;
        this.cacheRepository = cacheRepository;
        this.keyFactory = keyFactory;
        this.objectMapper = objectMapper;
        this.searchTtl = searchTtl;
        this.suggestTtl = suggestTtl;

        // Cache hit/miss counters (tagged by operation for dashboard filtering)
        this.searchCacheHits = Counter.builder("search.cache.hits")
                .description("Number of Redis cache hits")
                .tag("operation", "search")
                .register(meterRegistry);
        this.searchCacheMisses = Counter.builder("search.cache.misses")
                .description("Number of Redis cache misses")
                .tag("operation", "search")
                .register(meterRegistry);
        this.suggestCacheHits = Counter.builder("search.cache.hits")
                .description("Number of Redis cache hits")
                .tag("operation", "suggest")
                .register(meterRegistry);
        this.suggestCacheMisses = Counter.builder("search.cache.misses")
                .description("Number of Redis cache misses")
                .tag("operation", "suggest")
                .register(meterRegistry);

        // Elasticsearch query duration timers
        this.searchEsTimer = Timer.builder("search.elasticsearch.query.duration")
                .description("Time spent querying Elasticsearch")
                .tag("operation", "search")
                .register(meterRegistry);
        this.suggestEsTimer = Timer.builder("search.elasticsearch.query.duration")
                .description("Time spent querying Elasticsearch")
                .tag("operation", "suggest")
                .register(meterRegistry);
    }

    public List<SearchDocument> search(String query) {
        String normalizedQuery = keyFactory.normalize(query);
        String cacheKey = keyFactory.searchKey(normalizedQuery);

        Optional<List<SearchDocument>> cachedResults = readCache(cacheKey, SEARCH_RESULT_TYPE);
        if (cachedResults.isPresent()) {
            log.debug("Cache hit for search query key {}", cacheKey);
            searchCacheHits.increment();
            return cachedResults.get();
        }

        log.debug("Cache miss for search query key {}", cacheKey);
        searchCacheMisses.increment();
        List<SearchDocument> results = searchEsTimer.record(
                () -> elasticsearchSearchRepository.search(normalizedQuery)
        );
        writeCache(cacheKey, results, searchTtl);
        return results;
    }

    public List<String> suggest(String query) {
        String normalizedQuery = keyFactory.normalize(query);
        String cacheKey = keyFactory.suggestKey(normalizedQuery);

        Optional<List<String>> cachedSuggestions = readCache(cacheKey, SUGGESTION_RESULT_TYPE);
        if (cachedSuggestions.isPresent()) {
            log.debug("Cache hit for suggestion key {}", cacheKey);
            suggestCacheHits.increment();
            return cachedSuggestions.get();
        }

        log.debug("Cache miss for suggestion key {}", cacheKey);
        suggestCacheMisses.increment();
        List<String> suggestions = suggestEsTimer.record(
                () -> elasticsearchSearchRepository.suggest(normalizedQuery)
        );
        writeCache(cacheKey, suggestions, suggestTtl);
        return suggestions;
    }

    private <T> Optional<T> readCache(String key, TypeReference<T> typeReference) {
        try {
            Optional<String> cachedValue = cacheRepository.get(key);
            if (cachedValue.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(cachedValue.get(), typeReference));
        } catch (Exception e) {
            log.warn("Unable to read search cache key {}. Falling back to Elasticsearch.", key, e);
            return Optional.empty();
        }
    }

    private void writeCache(String key, Object value, Duration ttl) {
        try {
            cacheRepository.set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Unable to write search cache key {}. Returning Elasticsearch response without cache.", key, e);
        }
    }
}

