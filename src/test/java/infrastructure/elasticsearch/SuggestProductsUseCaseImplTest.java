package infrastructure.elasticsearch;

import infrastructure.search.CachedSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SuggestProductsUseCaseImplTest {

    private CachedSearchService cachedSearchService;
    private SuggestProductsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        cachedSearchService = mock(CachedSearchService.class);
        useCase = new SuggestProductsUseCaseImpl(cachedSearchService);
    }

    @Test
    void execute_delegatesToCachedSearchService() {
        List<String> expected = List.of("Phone Case", "Phone Charger");
        when(cachedSearchService.suggest("phone")).thenReturn(expected);

        List<String> result = useCase.execute("phone");

        assertEquals(expected, result);
        verify(cachedSearchService).suggest("phone");
    }

    @Test
    void execute_returnsEmptyList_whenNoSuggestionsFound() {
        when(cachedSearchService.suggest("xyz")).thenReturn(List.of());

        List<String> result = useCase.execute("xyz");

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_returnsWhateverCachedSearchServiceReturns() {
        List<String> suggestions = List.of("Keyboard", "Keyboard Cover");
        when(cachedSearchService.suggest("key")).thenReturn(suggestions);

        assertEquals(suggestions, useCase.execute("key"));
    }
}
