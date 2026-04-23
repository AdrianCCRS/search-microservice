package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisSearchCacheRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisSearchCacheRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        valueOperations = mockValueOperations();
        repository = new RedisSearchCacheRepository(redisTemplate);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getReturnsCachedValue() {
        when(valueOperations.get("search:query:key")).thenReturn("cached-json");

        Optional<String> cachedValue = repository.get("search:query:key");

        assertTrue(cachedValue.isPresent());
        assertEquals("cached-json", cachedValue.get());
    }

    @Test
    void setStoresValueWithTtl() {
        Duration ttl = Duration.ofMinutes(1);

        repository.set("search:query:key", "cached-json", ttl);

        verify(valueOperations).set("search:query:key", "cached-json", ttl);
    }

    @Test
    void deleteRemovesKey() {
        repository.delete("search:query:key");

        verify(redisTemplate).delete("search:query:key");
    }

    @Test
    void setRejectsInvalidTtl() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.set("search:query:key", "cached-json", Duration.ZERO));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ValueOperations<String, String> mockValueOperations() {
        return org.mockito.Mockito.mock(ValueOperations.class);
    }
}
