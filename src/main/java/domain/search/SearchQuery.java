package domain.search;

/**
 * Read-side search parameters (pagination + sort). Text filters can be added later on this type.
 */
public record SearchQuery(int page, int pageSize, SearchSort sort) {

    public static final int MAX_PAGE_SIZE = 50;

    public SearchQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sort == null) {
            throw new IllegalArgumentException("sort is required");
        }
    }

    public int fromIndex() {
        return page * pageSize;
    }
}
