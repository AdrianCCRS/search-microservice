package infrastructure.elasticsearch;

import domain.entities.SearchDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ElasticsearchSearchRepositoryIT {

    @Autowired
    private ElasticsearchSearchRepository repository;

    @Test
    void searchReturnsResultsForFullTextQuery() {

        SearchDocument doc = new SearchDocument(
                "prod-it-1",
                "iPhone 15",
                "Apple smartphone",
                "Electronics",
                BigDecimal.valueOf(1200),
                4.8,
                true,
                "Apple"
        );

        repository.indexDocument(doc);

        List<SearchDocument> results = repository.search("iphone");

        assertFalse(results.isEmpty());

        assertTrue(results.stream()
                .anyMatch(r -> "iPhone 15".equals(r.getName())));
    }

    @Test
    void searchSupportsFuzzyQueries() {

        SearchDocument doc = new SearchDocument(
                "prod-it-2",
                "Samsung Galaxy",
                "Android smartphone",
                "Electronics",
                BigDecimal.valueOf(900),
                4.5,
                true,
                "Samsung"
        );

        repository.indexDocument(doc);

        List<SearchDocument> results = repository.search("samsng");

        assertFalse(results.isEmpty());
    }
}