package application.ports;

import application.dto.SearchPageResult;
import domain.search.SearchQuery;

public interface ProductSearchPort {

    SearchPageResult search(SearchQuery query);
}
