package presentation.controllers;

import application.usecases.SearchProductsUseCase;
import domain.entities.SearchDocument;
import domain.queries.SearchQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchProductsUseCase searchProductsUseCase;

    public SearchController(SearchProductsUseCase searchProductsUseCase) {
        this.searchProductsUseCase = searchProductsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<SearchDocument>> search(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        SearchQuery query = new SearchQuery(
                term, category, minPrice, maxPrice, available, minRating, sortBy, sortDirection
        );
        
        List<SearchDocument> results = searchProductsUseCase.execute(query);
        return ResponseEntity.ok(results);
    }
}
