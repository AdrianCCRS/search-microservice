package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.search.CachedSearchService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import application.dto.ProductDTO;
import application.dto.SearchResponseDTO;

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

    @GetMapping("/v2")
    public SearchResponseDTO searchV2(@RequestParam("q") String query) {

        validateQuery(query);

        List<SearchDocument> results = cachedSearchService.search(query);

        List<ProductDTO> products = results.stream()
                .map(doc -> new ProductDTO(
                        doc.getId(),
                        doc.getName(),
                        doc.getDescription(),
                        doc.getBrand(),
                        doc.getScore() != null ? doc.getScore() : 0.0
                ))
                .collect(Collectors.toList());

        return new SearchResponseDTO(
                products.size(),
                1,
                products.size(),
                products
        );
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

        if (query != null && query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must have at least 2 characters");
        }
    }
}