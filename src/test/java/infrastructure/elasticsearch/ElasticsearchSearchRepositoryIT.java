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

        assertTrue(results.stream()
                .anyMatch(r ->
                        r.getName().contains("Samsung")
                ));
    }

    @Test
    void suggestReturnsMatchingProducts() {

        SearchDocument doc = new SearchDocument(
                "prod-it-3",
                "PlayStation 5",
                "Sony gaming console",
                "Gaming",
                BigDecimal.valueOf(700),
                4.9,
                true,
                "Sony"
        );

        repository.indexDocument(doc);

        List<String> suggestions = repository.suggest("Play");

        assertFalse(suggestions.isEmpty());

        assertTrue(suggestions.stream()
                .anyMatch(s -> s.contains("PlayStation")));
    }

    @Test
    void searchOrdersResultsByRelevance() {

        SearchDocument exactMatch = new SearchDocument(
                "prod-it-4",
                "Gaming Laptop",
                "High-end gaming laptop",
                "Electronics",
                BigDecimal.valueOf(2000),
                4.9,
                true,
                "Asus"
        );

        SearchDocument weakMatch = new SearchDocument(
                "prod-it-5",
                "Office Computer",
                "Laptop for office tasks",
                "Electronics",
                BigDecimal.valueOf(800),
                4.2,
                true,
                "HP"
        );

        repository.indexDocument(exactMatch);
        repository.indexDocument(weakMatch);

        List<SearchDocument> results = repository.search("gaming laptop");

        assertFalse(results.isEmpty());

        assertEquals("Gaming Laptop", results.get(0).getName());
    }
}