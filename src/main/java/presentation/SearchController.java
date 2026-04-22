package presentation;

import application.dto.SearchPageResult;
import application.usecases.SearchProductsUseCase;
import domain.search.SearchQuery;
import domain.search.SearchSort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class SearchController {

    private final SearchProductsUseCase searchProductsUseCase;

    public SearchController(SearchProductsUseCase searchProductsUseCase) {
        this.searchProductsUseCase = searchProductsUseCase;
    }

    /**
     * Paginated listing over the search index (match_all). Sort: price_asc, price_desc, rating_desc, popularity.
     */
    @GetMapping("/search")
    public SearchPageResult search(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(defaultValue = "popularity") String sort
    ) {
        SearchSort sortEnum = SearchSort.fromQueryParam(sort);
        SearchQuery query = new SearchQuery(page, pageSize, sortEnum);
        return searchProductsUseCase.execute(query);
    }
}
