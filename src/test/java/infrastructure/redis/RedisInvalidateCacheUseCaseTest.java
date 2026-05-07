package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import application.events.ProductData;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(OutputCaptureExtension.class)
class RedisInvalidateCacheUseCaseTest {

    private RedisSearchCacheRepository cacheRepository;
    private RedisInvalidateCacheUseCase useCase;

    @BeforeEach
    void setUp() {
        cacheRepository = org.mockito.Mockito.mock(RedisSearchCacheRepository.class);
        useCase = new RedisInvalidateCacheUseCase(cacheRepository, new SearchCacheKeyFactory());
        ReflectionTestUtils.setField(useCase, "queryKeyPattern", "search:query:*");
        ReflectionTestUtils.setField(useCase, "suggestKeyPattern", "search:suggest:*");
    }

    @Test
    void invalidateProductUpdatedDeletesQueryAndMatchingSuggestionPatterns(CapturedOutput output) {
        ProductData data = productData("  Gaming   Laptop  ");
        when(cacheRepository.deleteByPattern("search:query:*")).thenReturn(2L);
        when(cacheRepository.deleteByPattern("search:suggest:gaming laptop*")).thenReturn(1L);

        useCase.invalidateProductUpdated(data);

        verify(cacheRepository).deleteByPattern("search:query:*");
        verify(cacheRepository).deleteByPattern("search:suggest:gaming laptop*");
        assertTrue(output.getOut().contains("Invalidadas 3 claves de caché por evento ProductUpdated"));
    }

    @Test
    void invalidateProductUpdatedFallsBackToAllSuggestionsWhenNameIsBlank() {
        ProductData data = productData(" ");

        useCase.invalidateProductUpdated(data);

        verify(cacheRepository).deleteByPattern("search:query:*");
        verify(cacheRepository).deleteByPattern("search:suggest:*");
    }

    @Test
    void invalidateProductUpdatedFallsBackToAllSuggestionsWhenNameIsNull() {
        ProductData data = productData(null);

        useCase.invalidateProductUpdated(data);

        verify(cacheRepository).deleteByPattern("search:query:*");
        verify(cacheRepository).deleteByPattern("search:suggest:*");
    }

    @Test
    void invalidateProductCreatedDeletesAllSuggestionCacheKeys(CapturedOutput output) {
        ProductData data = productData("Laptop Gaming X");
        when(cacheRepository.deleteByPattern("search:suggest:*")).thenReturn(3L);

        useCase.invalidateProductCreated(data);

        verify(cacheRepository).deleteByPattern("search:suggest:*");
        verify(cacheRepository, never()).deleteByPattern("search:query:*");
        assertTrue(output.getOut().contains("Invalidadas 3 claves de caché por evento ProductCreated"));
    }

    @Test
    void invalidateProductUpdatedRejectsNullData() {
        assertThrows(IllegalArgumentException.class, () -> useCase.invalidateProductUpdated(null));

        verifyNoInteractions(cacheRepository);
    }

    @Test
    void invalidateProductCreatedRejectsNullData() {
        assertThrows(IllegalArgumentException.class, () -> useCase.invalidateProductCreated(null));

        verifyNoInteractions(cacheRepository);
    }

    private ProductData productData(String name) {
        return new ProductData("123", name, "Desc", "Cat", 10.0, 5.0, true, "Brand");
    }
}
