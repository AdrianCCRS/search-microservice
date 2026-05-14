package infrastructure.elasticsearch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EsQuerySanitizerTest {

    private final EsQuerySanitizer sanitizer = new EsQuerySanitizer();

    @Test
    void sanitizeRemovesAsteriskWildcard() {
        assertEquals("test", sanitizer.sanitize("*test*"));
    }

    @Test
    void sanitizeRemovesQuestionMark() {
        assertEquals("test", sanitizer.sanitize("test?"));
    }

    @Test
    void sanitizeRemovesTildeAndCaret() {
        assertEquals("foo bar baz", sanitizer.sanitize("foo~bar^baz"));
    }

    @Test
    void sanitizeRemovesBracesAndBrackets() {
        assertEquals("query", sanitizer.sanitize("{query}"));
        assertEquals("query", sanitizer.sanitize("[query]"));
        assertEquals("query", sanitizer.sanitize("(query)"));
    }

    @Test
    void sanitizeRemovesJsonStructureCharacters() {
        assertEquals("bool must match field value", sanitizer.sanitize("{\"bool\":{\"must\":[{\"match\":{\"field\":\"value\"}}]}}"));
    }

    @Test
    void sanitizeRemovesQueryStringOperators() {
        assertEquals("foo bar", sanitizer.sanitize("foo+bar"));
        assertEquals("foo bar", sanitizer.sanitize("foo-bar"));
        assertEquals("foo bar baz", sanitizer.sanitize("foo=bar&baz"));
        assertEquals("foo bar baz", sanitizer.sanitize("foo||bar&&baz"));
        assertEquals("foo bar", sanitizer.sanitize("foo>bar"));
        assertEquals("foo bar", sanitizer.sanitize("foo<bar"));
        assertEquals("foo", sanitizer.sanitize("!foo"));
    }

    @Test
    void sanitizeRemovesColonSlashAndBackslash() {
        assertEquals("field value", sanitizer.sanitize("field:value"));
        assertEquals("path to", sanitizer.sanitize("path/to"));
        assertEquals("path to", sanitizer.sanitize("path\\to"));
    }

    @Test
    void sanitizeNormalizesWhitespace() {
        assertEquals("hello world", sanitizer.sanitize("hello   world"));
        assertEquals("hello world", sanitizer.sanitize("hello~^world"));
    }

    @Test
    void sanitizeTrimsResult() {
        assertEquals("hello", sanitizer.sanitize("  hello  "));
    }

    @Test
    void sanitizeReturnsEmptyForNullInput() {
        assertEquals("", sanitizer.sanitize(null));
    }

    @Test
    void sanitizePreservesAlphanumericAndSpaces() {
        assertEquals("laptop gaming dell 2023", sanitizer.sanitize("laptop gaming dell 2023"));
    }

    @Test
    void sanitizePreservesSpanishCharacters() {
        assertEquals("niño búsqueda cañón", sanitizer.sanitize("niño búsqueda cañón"));
    }

    @Test
    void sanitizePreservesEmptyString() {
        assertEquals("", sanitizer.sanitize(""));
    }

    @Test
    void sanitizeReturnsAllSpacesForOnlySpecialChars() {
        assertEquals("", sanitizer.sanitize("***~~~^^^"));
    }
}
