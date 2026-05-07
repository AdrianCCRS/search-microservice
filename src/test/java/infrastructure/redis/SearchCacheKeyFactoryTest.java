package infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchCacheKeyFactoryTest {

    private final SearchCacheKeyFactory keyFactory = new SearchCacheKeyFactory();

    @Test
    void searchKeyUsesHashFormat() {
        String key = keyFactory.searchKey("  Gaming   Laptop ");

        assertTrue(key.matches("search:query:[a-f0-9]{64}"));
    }

    @Test
    void suggestKeyUsesNormalizedQueryFormat() {
        String key = keyFactory.suggestKey("  Gaming   Laptop ");

        assertEquals("search:suggest:gaming laptop", key);
    }
}
