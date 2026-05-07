package domain.search;

import java.util.List;

/**
 * Read-side search parameters (pagination + sort + cursor).
 *
 * <p>Supports two pagination strategies:
 * <ul>
 *   <li><b>Offset</b> ({@code from/size}): allowed only when {@code page <= 10} and no cursor is provided.
 *   <li><b>Cursor</b> ({@code search_after}): used when {@code searchAfter} is present; scales to any depth.
 * </ul>
 *
 * <p>Requesting {@code page > 10} without a cursor will throw {@link IllegalArgumentException}.
 */
public record SearchQuery(int page, int pageSize, SearchSort sort, List<String> searchAfter) {

    /** Maximum page reachable via offset pagination (without a search_after cursor). */
    public static final int MAX_OFFSET_PAGE = 10;
    public static final int MAX_PAGE_SIZE   = 50;

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
        // Offset pagination is capped; deep pages require a search_after cursor.
        if (page > MAX_OFFSET_PAGE && (searchAfter == null || searchAfter.isEmpty())) {
            throw new IllegalArgumentException(
                "page > " + MAX_OFFSET_PAGE + " is only allowed when a searchAfter cursor is provided. " +
                "Use the nextSearchAfter value from the previous response."
            );
        }
    }

    /** Convenience constructor that keeps backward-compatibility (no cursor). */
    public SearchQuery(int page, int pageSize, SearchSort sort) {
        this(page, pageSize, sort, null);
    }

    /** Whether this query uses cursor-based pagination. */
    public boolean hasCursor() {
        return searchAfter != null && !searchAfter.isEmpty();
    }

    /** Offset to use when falling back to from/size pagination. */
    public int fromIndex() {
        return page * pageSize;
    }
}
