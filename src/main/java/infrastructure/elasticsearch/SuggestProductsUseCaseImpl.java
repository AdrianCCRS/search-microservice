package infrastructure.elasticsearch;

import application.usecases.SuggestProductsUseCase;
import infrastructure.cache.RedisSearchCacheRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SuggestProductsUseCaseImpl implements SuggestProductsUseCase {

    private static final int MAX_SUGGESTIONS = 10;

    private final ElasticsearchSearchRepository searchRepository;
    private final RedisSearchCacheRepository cacheRepository;

    public SuggestProductsUseCaseImpl(
            ElasticsearchSearchRepository searchRepository,
            RedisSearchCacheRepository cacheRepository) {
        this.searchRepository = searchRepository;
        this.cacheRepository = cacheRepository;
    }

    @Override
    public List<String> execute(String query) {
        Optional<List<String>> cached = tryGetFromCache(query);
        if (cached.isPresent()) {
            System.out.println("Cache hit para sugerencias: " + query);
            return cached.get();
        }

        List<String> suggestions = searchRepository.suggest(query, MAX_SUGGESTIONS);
        trySaveToCache(query, suggestions);
        return suggestions;
    }

    private Optional<List<String>> tryGetFromCache(String query) {
        try {
            return cacheRepository.getSuggestions(query);
        } catch (Exception e) {
            System.err.println("Redis no disponible, fallback a Elasticsearch: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void trySaveToCache(String query, List<String> suggestions) {
        try {
            cacheRepository.setSuggestions(query, suggestions);
        } catch (Exception e) {
            System.err.println("No se pudo guardar en Redis: " + e.getMessage());
        }
    }
}
