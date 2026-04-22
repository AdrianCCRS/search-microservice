package domain.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchSortTest {

    @Test
    void defaultsToPopularityWhenBlank() {
        assertEquals(SearchSort.POPULARITY, SearchSort.fromQueryParam(null));
        assertEquals(SearchSort.POPULARITY, SearchSort.fromQueryParam(""));
        assertEquals(SearchSort.POPULARITY, SearchSort.fromQueryParam("   "));
    }

    @Test
    void parsesCaseInsensitiveAndHyphens() {
        assertEquals(SearchSort.PRICE_ASC, SearchSort.fromQueryParam("PRICE_ASC"));
        assertEquals(SearchSort.PRICE_DESC, SearchSort.fromQueryParam("price-desc"));
        assertEquals(SearchSort.RATING_DESC, SearchSort.fromQueryParam("rating_desc"));
        assertEquals(SearchSort.RATING_DESC, SearchSort.fromQueryParam("rating"));
        assertEquals(SearchSort.POPULARITY, SearchSort.fromQueryParam("Popularity"));
    }

    @Test
    void rejectsUnknownSort() {
        assertThrows(IllegalArgumentException.class, () -> SearchSort.fromQueryParam("name_asc"));
    }
}
