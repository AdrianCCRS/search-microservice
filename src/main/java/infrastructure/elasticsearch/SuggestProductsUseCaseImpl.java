package infrastructure.elasticsearch;

import application.usecases.SuggestProductsUseCase;
import infrastructure.search.CachedSearchService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Legacy-profile implementation of {@link SuggestProductsUseCase}.
 * Delegates to {@link CachedSearchService} which owns the canonical
 * Redis caching + Elasticsearch query logic, avoiding duplication.
 */
@Service
public class SuggestProductsUseCaseImpl implements SuggestProductsUseCase {

    private final CachedSearchService cachedSearchService;

    public SuggestProductsUseCaseImpl(CachedSearchService cachedSearchService) {
        this.cachedSearchService = cachedSearchService;
    }

    @Override
    public List<String> execute(String query) {
        return cachedSearchService.suggest(query);
    }
}
