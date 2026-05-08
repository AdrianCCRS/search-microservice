package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.search.CachedSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final CachedSearchService cachedSearchService;

    @GetMapping
    public List<SearchDocument> search(
            @RequestParam("q") String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchAfter", required = false) String searchAfter
    ) {
        validateQuery(query);
        validatePagination(page, searchAfter);
        return cachedSearchService.search(query);
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String query) {
        validateQuery(query);
        return cachedSearchService.suggest(query);
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'q' es requerido");
        }
    }

    private void validatePagination(Integer page, String searchAfter) {
        if (page != null && page > 100 && (searchAfter == null || searchAfter.isBlank())) {
            throw new IllegalArgumentException("Para page > 100 debes enviar searchAfter");
        }
    }
}
