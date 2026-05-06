package application.usecases;

import infrastructure.elasticsearch.ElasticsearchSearchRepository.SearchResult;
import domain.queries.SearchQuery;

public interface SearchProductsUseCase {
    SearchResult execute(SearchQuery query);
}
