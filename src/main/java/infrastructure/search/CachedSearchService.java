package infrastructure.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entities.SearchDocument;
import infrastructure.elasticsearch.ElasticsearchSearchRepository;
import infrastructure.redis.RedisSearchCacheRepository;
import infrastructure.redis.SearchCacheKeyFactory;
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

    public CachedSearchService(
            ElasticsearchSearchRepository elasticsearchSearchRepository,
            RedisSearchCacheRepository cacheRepository,
            SearchCacheKeyFactory keyFactory,
            ObjectMapper objectMapper,
            @Value("${search.cache.redis.search-ttl:1m}") Duration searchTtl,
            @Value("${search.cache.redis.suggest-ttl:5m}") Duration suggestTtl) {
        this.elasticsearchSearchRepository = elasticsearchSearchRepository;
        this.cacheRepository = cacheRepository;
        this.keyFactory = keyFactory;
        this.objectMapper = objectMapper;
        this.searchTtl = searchTtl;
        this.suggestTtl = suggestTtl;
    }

    public List<SearchDocument> search(String query) {
        String normalizedQuery = keyFactory.normalize(query);
        String cacheKey = keyFactory.searchKey(normalizedQuery);

        Optional<List<SearchDocument>> cachedResults = readCache(cacheKey, SEARCH_RESULT_TYPE);
        if (cachedResults.isPresent()) {
            log.debug("Cache hit for search query key {}", cacheKey);
            return cachedResults.get();
        }

        log.debug("Cache miss for search query key {}", cacheKey);
        List<SearchDocument> results = elasticsearchSearchRepository.search(normalizedQuery);
        writeCache(cacheKey, results, searchTtl);
        return results;
    }

    public List<String> suggest(String query) {
        String normalizedQuery = keyFactory.normalize(query);
        String cacheKey = keyFactory.suggestKey(normalizedQuery);

        Optional<List<String>> cachedSuggestions = readCache(cacheKey, SUGGESTION_RESULT_TYPE);
        if (cachedSuggestions.isPresent()) {
            log.debug("Cache hit for suggestion key {}", cacheKey);
            return cachedSuggestions.get();
        }

        log.debug("Cache miss for suggestion key {}", cacheKey);
        List<String> suggestions = elasticsearchSearchRepository.suggest(normalizedQuery, 10);
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
