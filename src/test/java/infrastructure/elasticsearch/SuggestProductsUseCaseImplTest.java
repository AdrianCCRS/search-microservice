package infrastructure.elasticsearch;

import infrastructure.cache.RedisSearchCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SuggestProductsUseCaseImplTest {

    private ElasticsearchSearchRepository searchRepository;
    private RedisSearchCacheRepository cacheRepository;
    private SuggestProductsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        searchRepository = mock(ElasticsearchSearchRepository.class);
        cacheRepository = mock(RedisSearchCacheRepository.class);
        useCase = new SuggestProductsUseCaseImpl(searchRepository, cacheRepository);
    }

    @Test
    void returnsCachedSuggestionsOnCacheHit() {
        List<String> cached = List.of("Phone Case", "Phone Charger");
        when(cacheRepository.getSuggestions("phone")).thenReturn(Optional.of(cached));

        List<String> result = useCase.execute("phone");

        assertEquals(cached, result);
        verifyNoInteractions(searchRepository);
        verify(cacheRepository, never()).setSuggestions(any(), any());
    }

    @Test
    void queriesElasticsearchAndCachesOnCacheMiss() {
        List<String> suggestions = List.of("Phone Case", "Phone Charger");
        when(cacheRepository.getSuggestions("phone")).thenReturn(Optional.empty());
        when(searchRepository.suggest("phone", 10)).thenReturn(suggestions);

        List<String> result = useCase.execute("phone");

        assertEquals(suggestions, result);
        verify(searchRepository).suggest("phone", 10);
        verify(cacheRepository).setSuggestions("phone", suggestions);
    }

    @Test
    void fallsBackToElasticsearchWhenRedisThrows() {
        List<String> suggestions = List.of("Mouse", "Mouse Pad");
        when(cacheRepository.getSuggestions("mouse")).thenThrow(new RuntimeException("Redis down"));
        when(searchRepository.suggest("mouse", 10)).thenReturn(suggestions);

        List<String> result = useCase.execute("mouse");

        assertEquals(suggestions, result);
        verify(searchRepository).suggest("mouse", 10);
    }

    @Test
    void continuesEvenWhenCacheWriteFails() {
        List<String> suggestions = List.of("Keyboard");
        when(cacheRepository.getSuggestions("key")).thenReturn(Optional.empty());
        when(searchRepository.suggest("key", 10)).thenReturn(suggestions);
        doThrow(new RuntimeException("Redis write error")).when(cacheRepository).setSuggestions(any(), any());

        List<String> result = useCase.execute("key");

        assertEquals(suggestions, result);
    }
}
