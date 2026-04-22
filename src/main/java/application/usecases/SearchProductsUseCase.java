package application.usecases;

import application.dto.SearchPageResult;
import domain.search.SearchQuery;

public interface SearchProductsUseCase {

    SearchPageResult execute(SearchQuery query);
}
