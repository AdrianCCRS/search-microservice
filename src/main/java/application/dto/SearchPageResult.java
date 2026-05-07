package application.dto;

import domain.entities.SearchDocument;

import java.util.List;

/**
 * Paginated search outcome: total hit count from Elasticsearch plus the resolved page metadata.
 *
 * <p>{@code nextSearchAfter} is the cursor for the next page when using search_after pagination.
 * It will be {@code null} when there are no more pages or when offset pagination was used.
 */
public record SearchPageResult(
    long totalResults,
    int page,
    int pageSize,
    List<String> nextSearchAfter,
    List<SearchDocument> items
) {}
