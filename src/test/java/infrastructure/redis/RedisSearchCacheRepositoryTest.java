package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisSearchCacheRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisSearchCacheRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        valueOperations = mockValueOperations();
        repository = new RedisSearchCacheRepository(redisTemplate);
        ReflectionTestUtils.setField(repository, "scanCount", 1000L);
        ReflectionTestUtils.setField(repository, "unlinkBatchSize", 500);

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

    @Test
    void deleteByPatternScansAndUnlinksMatchingKeys() throws Exception {
        Cursor<String> cursor = mockCursor();
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("search:query:one", "search:query:two");
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.unlink(List.of("search:query:one", "search:query:two"))).thenReturn(2L);

        long deleted = repository.deleteByPattern("search:query:*");

        assertEquals(2L, deleted);
        ArgumentCaptor<ScanOptions> optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate).scan(optionsCaptor.capture());
        assertEquals("search:query:*", optionsCaptor.getValue().getPattern());
        verify(redisTemplate).unlink(List.of("search:query:one", "search:query:two"));
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void deleteByPatternUnlinksKeysInBatchesAndReturnsTotal() throws Exception {
        ReflectionTestUtils.setField(repository, "unlinkBatchSize", 2);
        Cursor<String> cursor = mockCursor();
        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next()).thenReturn("search:query:one", "search:query:two", "search:query:three");
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        List<List<String>> unlinkedBatches = new ArrayList<>();
        doAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            unlinkedBatches.add(List.copyOf(keys));
            return Long.valueOf(keys.size());
        }).when(redisTemplate).unlink(any(List.class));

        long deleted = repository.deleteByPattern("search:query:*");

        assertEquals(3L, deleted);
        assertEquals(List.of(
                List.of("search:query:one", "search:query:two"),
                List.of("search:query:three")), unlinkedBatches);
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void deleteByPatternReturnsZeroWhenNoKeysMatch() throws Exception {
        Cursor<String> cursor = mockCursor();
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        long deleted = repository.deleteByPattern("search:query:*");

        assertEquals(0L, deleted);
        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).unlink(any(List.class));
    }

    @Test
    void deleteByPatternRejectsNullEmptyOrBlankPattern() {
        assertThrows(IllegalArgumentException.class, () -> repository.deleteByPattern(null));
        assertThrows(IllegalArgumentException.class, () -> repository.deleteByPattern(""));
        assertThrows(IllegalArgumentException.class, () -> repository.deleteByPattern(" "));

        verify(redisTemplate, never()).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).unlink(any(List.class));
        verify(redisTemplate, never()).delete(any(String.class));
    }

    private ValueOperations<String, String> mockValueOperations() {
        return org.mockito.Mockito.mock(ValueOperations.class);
    }

    private Cursor<String> mockCursor() {
        return org.mockito.Mockito.mock(Cursor.class);
    }
}
