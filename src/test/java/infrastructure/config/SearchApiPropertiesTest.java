package infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchApiPropertiesTest {

    @Test
    void defaultValues_matchExpectedTunables() {
        SearchApiProperties props = new SearchApiProperties();

        assertEquals(200, props.getQueryMaxLength());
        assertEquals(100, props.getPaginationMaxPageWithoutSearchAfter());
        assertEquals(10, props.getEsSuggestMaxExpansions());
    }

    @Test
    void setters_updateFieldsCorrectly() {
        SearchApiProperties props = new SearchApiProperties();

        props.setQueryMaxLength(500);
        props.setPaginationMaxPageWithoutSearchAfter(50);
        props.setEsSuggestMaxExpansions(20);

        assertEquals(500, props.getQueryMaxLength());
        assertEquals(50, props.getPaginationMaxPageWithoutSearchAfter());
        assertEquals(20, props.getEsSuggestMaxExpansions());
    }
}
