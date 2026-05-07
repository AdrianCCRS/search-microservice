package infrastructure.elasticsearch;

import domain.entities.SearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SearchProductsUseCaseImplTest {

    private ElasticsearchSearchRepository repository;
    private SearchProductsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = mock(ElasticsearchSearchRepository.class);
        useCase = new SearchProductsUseCaseImpl(repository);
    }

    @Test
    void searchDelegatesToRepository() {
        SearchDocument doc = new SearchDocument(
                "prod-1", "Laptop", "Gaming", "Electronics",
                BigDecimal.valueOf(999.0), 4.5, true, "Dell");
        when(repository.search("laptop")).thenReturn(List.of(doc));

        List<SearchDocument> results = useCase.search("laptop");

        assertEquals(1, results.size());
        assertEquals("prod-1", results.get(0).getProductId());
        verify(repository).search("laptop");
    }

    @Test
    void searchReturnsEmptyListWhenNoResults() {
        when(repository.search("unknown")).thenReturn(List.of());

        List<SearchDocument> results = useCase.search("unknown");

        assertEquals(0, results.size());
        verify(repository).search("unknown");
    }
}
