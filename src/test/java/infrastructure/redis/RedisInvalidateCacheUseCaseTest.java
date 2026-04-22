package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class RedisInvalidateCacheUseCaseTest {

    private StringRedisTemplate redisTemplate;
    private RedisInvalidateCacheUseCase useCase;

    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        useCase = new RedisInvalidateCacheUseCase(redisTemplate);
        ReflectionTestUtils.setField(useCase, "productKeyPrefix", "search:product:");
        ReflectionTestUtils.setField(useCase, "queryKeyPattern", "search:query:*");
        ReflectionTestUtils.setField(useCase, "scanCount", 1000L);
    }

    @Test
    void executeDeletesProductAndQueryCacheKeys() throws Exception {
        Cursor<String> cursor = mockCursor();
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("search:query:name:phone", "search:query:category:tech");
        when(redisTemplate.scan(org.mockito.ArgumentMatchers.any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.<Collection<String>>any())).thenReturn(3L);

        useCase.execute("123");

        ArgumentCaptor<Collection<String>> keysCaptor = collectionCaptor();
        verify(redisTemplate).delete(keysCaptor.capture());

        Collection<String> deletedKeys = keysCaptor.getValue();
        assertTrue(deletedKeys.contains("search:product:123"));
        assertTrue(deletedKeys.contains("search:query:name:phone"));
        assertTrue(deletedKeys.contains("search:query:category:tech"));
        assertEquals(3, deletedKeys.size());
    }

    @Test
    void executeDeletesOnlyProductKeyWhenNoQueryKeysExist() throws Exception {
        Cursor<String> cursor = mockCursor();
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(org.mockito.ArgumentMatchers.any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.<Collection<String>>any())).thenReturn(1L);

        useCase.execute("123");

        ArgumentCaptor<Collection<String>> keysCaptor = collectionCaptor();
        verify(redisTemplate).delete(keysCaptor.capture());

        Collection<String> deletedKeys = keysCaptor.getValue();
        assertTrue(deletedKeys.contains("search:product:123"));
        assertEquals(1, deletedKeys.size());
    }

    @Test
    void executeRejectsBlankProductId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" "));

        verify(redisTemplate, never()).scan(org.mockito.ArgumentMatchers.any(ScanOptions.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Collection<String>> collectionCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Cursor<String> mockCursor() {
        return org.mockito.Mockito.mock(Cursor.class);
    }
}
