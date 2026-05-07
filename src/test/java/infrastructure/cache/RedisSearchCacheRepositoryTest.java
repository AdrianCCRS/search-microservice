package infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisSearchCacheRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisSearchCacheRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new RedisSearchCacheRepository(redisTemplate, new ObjectMapper());
    }

    @Test
    void getSuggestionsReturnsParsedListOnCacheHit() throws Exception {
        String key = "suggest:phone";
        String json = "[\"Phone Case\",\"Phone Charger\"]";
        when(valueOperations.get(key)).thenReturn(json);

        Optional<List<String>> result = repository.getSuggestions("phone");

        assertTrue(result.isPresent());
        assertEquals(List.of("Phone Case", "Phone Charger"), result.get());
    }

    @Test
    void getSuggestionsReturnsEmptyWhenKeyNotFound() {
        when(valueOperations.get("suggest:laptop")).thenReturn(null);

        Optional<List<String>> result = repository.getSuggestions("laptop");

        assertFalse(result.isPresent());
    }

    @Test
    void getSuggestionsReturnsEmptyOnInvalidJson() {
        when(valueOperations.get("suggest:bad")).thenReturn("not-valid-json");

        Optional<List<String>> result = repository.getSuggestions("bad");

        assertFalse(result.isPresent());
    }

    @Test
    void setSuggestionsStoresSerializedJsonWithTtl() throws Exception {
        List<String> suggestions = List.of("Mouse", "Mouse Pad");

        repository.setSuggestions("mouse", suggestions);

        verify(valueOperations).set(
                eq("suggest:mouse"),
                eq("[\"Mouse\",\"Mouse Pad\"]"),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void getSuggestionsBuildsKeyWithLowercaseQuery() {
        when(valueOperations.get("suggest:laptop")).thenReturn(null);

        repository.getSuggestions("Laptop");

        verify(valueOperations).get("suggest:laptop");
    }
}
