package application.dto;

import domain.entities.SearchDocument;

import java.util.List;

/**
 * Paginated search outcome: total hit count from Elasticsearch plus the resolved page metadata.
 */
public record SearchPageResult(
    long totalResults,
    int page,
    int pageSize,
    List<SearchDocument> items
) {}
