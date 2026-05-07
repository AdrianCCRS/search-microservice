package infrastructure.elasticsearch;

import application.events.ProductData;
import domain.entities.SearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class IndexProductUseCaseImplTest {

    private ElasticsearchSearchRepository repository;
    private IndexProductUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = mock(ElasticsearchSearchRepository.class);
        useCase = new IndexProductUseCaseImpl(repository);
    }

    @Test
    void executeDelegatesToRepositoryWithMappedDocument() {
        ProductData data = new ProductData(
                "prod-1", "Laptop", "Gaming laptop", "Electronics",
                1499.99, 4.7, true, "Asus");

        useCase.execute(data);

        verify(repository).indexDocument(argThat(doc ->
                "prod-1".equals(doc.getProductId()) &&
                "Laptop".equals(doc.getName()) &&
                "Gaming laptop".equals(doc.getDescription()) &&
                "Electronics".equals(doc.getCategory()) &&
                BigDecimal.valueOf(1499.99).compareTo(doc.getPrice()) == 0 &&
                4.7 == doc.getRating() &&
                Boolean.TRUE.equals(doc.getAvailable()) &&
                "Asus".equals(doc.getBrand())
        ));
    }

    @Test
    void executeWithNullPriceIndexesDocumentWithNullPrice() {
        ProductData data = new ProductData(
                "prod-2", "Keyboard", "Mechanical", "Accessories",
                null, 4.0, true, "Corsair");

        useCase.execute(data);

        verify(repository).indexDocument(argThat(doc ->
                "prod-2".equals(doc.getProductId()) && doc.getPrice() == null
        ));
    }

    @Test
    void executeThrowsWhenDataIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
        verifyNoInteractions(repository);
    }

    @Test
    void executeThrowsWhenProductIdIsNull() {
        ProductData data = new ProductData(
                null, "Laptop", "Desc", "Cat", 100.0, 4.0, true, "Brand");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(data));
        verifyNoInteractions(repository);
    }

    @Test
    void executeThrowsWhenProductIdIsBlank() {
        ProductData data = new ProductData(
                "   ", "Laptop", "Desc", "Cat", 100.0, 4.0, true, "Brand");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(data));
        verifyNoInteractions(repository);
    }
}
