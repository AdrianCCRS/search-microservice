package presentation;

import application.dto.SearchPageResult;
import application.usecases.SearchProductsUseCase;
import domain.search.SearchQuery;
import domain.search.SearchSort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class SearchController {

    private final SearchProductsUseCase searchProductsUseCase;

    public SearchController(SearchProductsUseCase searchProductsUseCase) {
        this.searchProductsUseCase = searchProductsUseCase;
    }

    /**
     * Paginated product search.
     *
     * <p>Supports two pagination modes:
     * <ul>
     *   <li><b>Offset</b>: use {@code page} and {@code pageSize} (max page = 10 without cursor).
     *   <li><b>Cursor</b>: pass {@code searchAfter} values from the previous {@code nextSearchAfter}
     *       response field to navigate deeper without performance degradation.
     * </ul>
     *
     * @param page        zero-based page number (default 0)
     * @param pageSize    results per page, 1–50 (default 10)
     * @param sort        ordering: price_asc, price_desc, rating_desc, popularity (default)
     * @param searchAfter cursor values from the previous page's {@code nextSearchAfter} field
     */
    @GetMapping("/search")
    public ResponseEntity<SearchPageResult> search(
        @RequestParam(defaultValue = "0")          int page,
        @RequestParam(defaultValue = "10")         int pageSize,
        @RequestParam(defaultValue = "popularity") String sort,
        @RequestParam(required = false)            List<String> searchAfter
    ) {
        SearchSort sortEnum = SearchSort.fromQueryParam(sort);
        // SearchQuery constructor validates page > MAX_OFFSET_PAGE without cursor → throws IllegalArgumentException → 400
        SearchQuery query = new SearchQuery(page, pageSize, sortEnum, searchAfter);
        SearchPageResult result = searchProductsUseCase.execute(query);
        return ResponseEntity.ok(result);
    }
}
