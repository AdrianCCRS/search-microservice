package domain.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SearchDocumentTest {

    @Test
    void allArgsConstructorSetsAllFields() {
        SearchDocument doc = new SearchDocument(
                "prod-1", "Laptop", "A gaming laptop",
                "Electronics", BigDecimal.valueOf(1500.00),
                4.5, true, "Asus");

        assertEquals("prod-1", doc.getProductId());
        assertEquals("Laptop", doc.getName());
        assertEquals("A gaming laptop", doc.getDescription());
        assertEquals("Electronics", doc.getCategory());
        assertEquals(BigDecimal.valueOf(1500.00), doc.getPrice());
        assertEquals(4.5, doc.getRating());
        assertTrue(doc.getAvailable());
        assertEquals("Asus", doc.getBrand());
    }

    @Test
    void defaultConstructorCreatesEmptyDocument() {
        SearchDocument doc = new SearchDocument();

        assertNull(doc.getProductId());
        assertNull(doc.getName());
        assertNull(doc.getDescription());
        assertNull(doc.getCategory());
        assertNull(doc.getPrice());
        assertNull(doc.getRating());
        assertNull(doc.getAvailable());
        assertNull(doc.getBrand());
    }

    @Test
    void settersUpdateFieldsCorrectly() {
        SearchDocument doc = new SearchDocument();
        doc.setProductId("prod-2");
        doc.setName("Mouse");
        doc.setDescription("Wireless mouse");
        doc.setCategory("Accessories");
        doc.setPrice(BigDecimal.valueOf(25.99));
        doc.setRating(4.0);
        doc.setAvailable(false);
        doc.setBrand("Logitech");

        assertEquals("prod-2", doc.getProductId());
        assertEquals("Mouse", doc.getName());
        assertEquals("Wireless mouse", doc.getDescription());
        assertEquals("Accessories", doc.getCategory());
        assertEquals(BigDecimal.valueOf(25.99), doc.getPrice());
        assertEquals(4.0, doc.getRating());
        assertFalse(doc.getAvailable());
        assertEquals("Logitech", doc.getBrand());
    }

    @Test
    void nullFieldsArePermitted() {
        SearchDocument doc = new SearchDocument(
                "prod-3", null, null, null, null, null, null, null);

        assertEquals("prod-3", doc.getProductId());
        assertNull(doc.getName());
        assertNull(doc.getPrice());
    }
}
