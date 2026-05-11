package infrastructure.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.entities.SearchDocument;
import infrastructure.elasticsearch.ElasticsearchSearchRepository;
import infrastructure.redis.RedisSearchCacheRepository;
import infrastructure.redis.SearchCacheKeyFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedSearchServiceTest {

    private ElasticsearchSearchRepository elasticsearchSearchRepository;
    private RedisSearchCacheRepository cacheRepository;
    private SearchCacheKeyFactory keyFactory;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private CachedSearchService service;

    @BeforeEach
    void setUp() {
        elasticsearchSearchRepository = org.mockito.Mockito.mock(ElasticsearchSearchRepository.class);
        cacheRepository = org.mockito.Mockito.mock(RedisSearchCacheRepository.class);
        keyFactory = new SearchCacheKeyFactory();
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        service = new CachedSearchService(
                elasticsearchSearchRepository,
                cacheRepository,
                keyFactory,
                objectMapper,
                meterRegistry,
                Duration.ofMinutes(1),
                Duration.ofMinutes(5)
        );
    }

    @Test
    void searchReturnsCachedResultOnCacheHit() throws Exception {
        SearchDocument document = document("123", "Gaming Laptop");
        String cacheKey = keyFactory.searchKey("gaming laptop");
        when(cacheRepository.get(cacheKey)).thenReturn(Optional.of(objectMapper.writeValueAsString(List.of(document))));

        List<SearchDocument> results = service.search("  Gaming   Laptop ");

        assertEquals(1, results.size());
        assertEquals("123", results.get(0).getProductId());
        verifyNoInteractions(elasticsearchSearchRepository);
        verify(cacheRepository, never()).set(eq(cacheKey), anyString(), eq(Duration.ofMinutes(1)));
    }

    @Test
    void searchQueriesElasticsearchAndCachesOnMiss() {
        SearchDocument document = document("123", "Gaming Laptop");
        String cacheKey = keyFactory.searchKey("gaming laptop");
        when(cacheRepository.get(cacheKey)).thenReturn(Optional.empty());
        when(elasticsearchSearchRepository.search("gaming laptop")).thenReturn(List.of(document));

        List<SearchDocument> results = service.search("Gaming Laptop");

        assertEquals(1, results.size());
        verify(elasticsearchSearchRepository).search("gaming laptop");
        verify(cacheRepository).set(eq(cacheKey), anyString(), eq(Duration.ofMinutes(1)));
    }

    @Test
    void suggestUsesFiveMinuteTtlOnMiss() {
        String cacheKey = keyFactory.suggestKey("phone");
        when(cacheRepository.get(cacheKey)).thenReturn(Optional.empty());
        when(elasticsearchSearchRepository.suggest("phone")).thenReturn(List.of("Phone Case", "Phone Charger"));

        List<String> results = service.suggest("Phone");

        assertEquals(2, results.size());
        verify(elasticsearchSearchRepository).suggest("phone");
        verify(cacheRepository).set(eq(cacheKey), anyString(), eq(Duration.ofMinutes(5)));
    }

    private SearchDocument document(String productId, String name) {
        return new SearchDocument(productId, name, "Description", "Category",
                BigDecimal.TEN, 5.0, true, "Brand");
    }
}
