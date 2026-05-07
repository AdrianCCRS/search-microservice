package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisInvalidateCacheUseCaseTest {

    private StringRedisTemplate redisTemplate;
    private RedisSearchCacheRepository cacheRepository;
    private RedisInvalidateCacheUseCase useCase;

    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        cacheRepository = org.mockito.Mockito.mock(RedisSearchCacheRepository.class);
        useCase = new RedisInvalidateCacheUseCase(redisTemplate, cacheRepository);
        ReflectionTestUtils.setField(useCase, "productKeyPrefix", "search:product:");
        ReflectionTestUtils.setField(useCase, "queryKeyPattern", "search:query:*");
        ReflectionTestUtils.setField(useCase, "suggestKeyPattern", "search:suggest:*");
        ReflectionTestUtils.setField(useCase, "scanCount", 1000L);
    }

    @Test
    void executeDeletesProductQueryAndSuggestionCacheKeys() throws Exception {
        Cursor<String> queryCursor = mockCursor();
        when(queryCursor.hasNext()).thenReturn(true, false);
        when(queryCursor.next()).thenReturn("search:query:abc123");

        Cursor<String> suggestCursor = mockCursor();
        when(suggestCursor.hasNext()).thenReturn(true, false);
        when(suggestCursor.next()).thenReturn("search:suggest:phone");

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(queryCursor, suggestCursor);

        useCase.execute("123");

        verify(cacheRepository).delete("search:product:123");
        verify(cacheRepository).delete("search:query:abc123");
        verify(cacheRepository).delete("search:suggest:phone");
    }

    @Test
    void executeDeletesOnlyProductKeyWhenNoSearchKeysExist() throws Exception {
        Cursor<String> queryCursor = mockCursor();
        when(queryCursor.hasNext()).thenReturn(false);

        Cursor<String> suggestCursor = mockCursor();
        when(suggestCursor.hasNext()).thenReturn(false);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(queryCursor, suggestCursor);

        useCase.execute("123");

        verify(cacheRepository).delete("search:product:123");
    }

    @Test
    void executeRejectsBlankProductId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" "));

        verify(redisTemplate, never()).scan(any(ScanOptions.class));
        verifyNoInteractions(cacheRepository);
    }

    private Cursor<String> mockCursor() {
        return org.mockito.Mockito.mock(Cursor.class);
    }
}
