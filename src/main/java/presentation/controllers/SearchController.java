package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.search.CachedSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final CachedSearchService cachedSearchService;

    @GetMapping
    public List<SearchDocument> search(@RequestParam("q") String query) {
        validateQuery(query);
        return cachedSearchService.search(query);
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String query) {
        validateQuery(query);
        return cachedSearchService.suggest(query);
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q query parameter is required");
        }
    }
}
