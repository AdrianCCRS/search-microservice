package application.usecases;

import domain.entities.SearchDocument;
import domain.queries.SearchQuery;
import java.util.List;

public interface SearchProductsUseCase {
    List<SearchDocument> execute(SearchQuery query);
}
