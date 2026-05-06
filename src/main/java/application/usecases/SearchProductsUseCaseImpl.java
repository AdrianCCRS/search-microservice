package application.usecases;

import domain.queries.SearchQuery;
import infrastructure.elasticsearch.ElasticsearchSearchRepository;
import infrastructure.elasticsearch.ElasticsearchSearchRepository.SearchResult;
import org.springframework.stereotype.Service;

@Service
public class SearchProductsUseCaseImpl implements SearchProductsUseCase {

    private final ElasticsearchSearchRepository repository;

    public SearchProductsUseCaseImpl(ElasticsearchSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public SearchResult execute(SearchQuery query) {
        return repository.search(query);
    }
}
