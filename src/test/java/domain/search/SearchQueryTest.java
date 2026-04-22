package domain.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchQueryTest {

    @Test
    void fromIndexUsesPageTimesPageSize() {
        SearchQuery q = new SearchQuery(2, 15, SearchSort.PRICE_ASC);
        assertEquals(30, q.fromIndex());
    }

    @Test
    void rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(-1, 10, SearchSort.POPULARITY));
    }

    @Test
    void rejectsPageSizeBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(0, 0, SearchSort.POPULARITY));
    }

    @Test
    void rejectsPageSizeAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(0, 51, SearchSort.POPULARITY));
    }

    @Test
    void acceptsMaxPageSize() {
        assertDoesNotThrow(() -> new SearchQuery(0, SearchQuery.MAX_PAGE_SIZE, SearchSort.PRICE_DESC));
    }

    @Test
    void rejectsNullSort() {
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(0, 10, null));
    }
}
