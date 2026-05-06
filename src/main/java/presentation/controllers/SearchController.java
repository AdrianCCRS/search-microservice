package presentation.controllers;

import application.usecases.SearchProductsUseCase;
import domain.entities.SearchDocument;
import domain.queries.SearchQuery;
import infrastructure.elasticsearch.ElasticsearchSearchRepository.SearchResult;
import infrastructure.elasticsearch.ElasticsearchSearchRepository.SearchResultItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import presentation.dto.SearchResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchProductsUseCase searchProductsUseCase;

    public SearchController(SearchProductsUseCase searchProductsUseCase) {
        this.searchProductsUseCase = searchProductsUseCase;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize debe estar entre 1 y 100");
        }

        SearchQuery query = new SearchQuery(
                term, category, minPrice, maxPrice, available, minRating,
                sortBy, sortDirection, page, pageSize
        );

        SearchResult result = searchProductsUseCase.execute(query);

        List<SearchResponse.ProductResult> products = result.getItems().stream()
                .map(item -> {
                    SearchDocument doc = item.getDocument();
                    return new SearchResponse.ProductResult(
                            doc.getProductId(),
                            doc.getName(),
                            doc.getDescription(),
                            doc.getCategory(),
                            doc.getPrice() != null ? doc.getPrice().doubleValue() : null,
                            doc.getRating(),
                            doc.getAvailable(),
                            doc.getBrand(),
                            item.getScore()
                    );
                })
                .collect(Collectors.toList());

        SearchResponse response = new SearchResponse(
                (int) result.getTotal(),
                result.getPage(),
                result.getPageSize(),
                products
        );

        return ResponseEntity.ok(response);
    }
}
