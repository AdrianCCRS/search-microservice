package domain.search;

/**
 * Supported result ordering for product search (Elasticsearch field sorts).
 */
public enum SearchSort {
    PRICE_ASC,
    PRICE_DESC,
    /** Higher rating first (same ordering as {@link #POPULARITY}). */
    RATING_DESC,
    /** Popularity: ordered by rating descending. */
    POPULARITY;

    public static SearchSort fromQueryParam(String value) {
        if (value == null || value.isBlank()) {
            return POPULARITY;
        }
        String normalized = value.trim().toLowerCase().replace('-', '_');
        return switch (normalized) {
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            case "rating_desc", "rating" -> RATING_DESC;
            case "popularity" -> POPULARITY;
            default -> throw new IllegalArgumentException(
                "Invalid sort '" + value + "'. Use: price_asc, price_desc, rating_desc, popularity");
        };
    }
}
