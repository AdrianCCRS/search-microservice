package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.config.SearchApiProperties;
import infrastructure.elasticsearch.EsQuerySanitizer;
import infrastructure.search.CachedSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final CachedSearchService cachedSearchService;
    private final EsQuerySanitizer esQuerySanitizer;
    private final SearchApiProperties searchApiProperties;

    @GetMapping
    public List<SearchDocument> search(
            @RequestParam("q")
            @NotBlank(message = "El parámetro 'q' es requerido")
            @Size(max = 200, message = "El parámetro 'q' no debe exceder 200 caracteres")
            String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "searchAfter", required = false) String searchAfter
    ) {
        validateQuery(query);
        validatePagination(page, searchAfter);
        String sanitized = esQuerySanitizer.sanitize(query);
        return cachedSearchService.search(sanitized);
    }

    @GetMapping("/suggest")
    public List<String> suggest(
            @RequestParam("q")
            @NotBlank(message = "El parámetro 'q' es requerido")
            @Size(max = 200, message = "El parámetro 'q' no debe exceder 200 caracteres")
            String query
    ) {
        validateQuery(query);
        String sanitized = esQuerySanitizer.sanitize(query);
        return cachedSearchService.suggest(sanitized);
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'q' es requerido");
        }
        if (query.length() > searchApiProperties.getQueryMaxLength()) {
            throw new IllegalArgumentException(
                    "El parámetro 'q' no debe exceder " + searchApiProperties.getQueryMaxLength() + " caracteres");
        }
    }

    private void validatePagination(Integer page, String searchAfter) {
        if (page != null
                && page > searchApiProperties.getPaginationMaxPageWithoutSearchAfter()
                && (searchAfter == null || searchAfter.isBlank())) {
            throw new IllegalArgumentException(
                    "Para page > " + searchApiProperties.getPaginationMaxPageWithoutSearchAfter()
                    + " debes enviar searchAfter");
        }
    }
}
