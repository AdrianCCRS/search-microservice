package application.usecases;

import domain.entities.SearchDocument;
import domain.queries.SearchQuery;
import infrastructure.elasticsearch.ElasticsearchSearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchProductsUseCaseImpl implements SearchProductsUseCase {

    private final ElasticsearchSearchRepository repository;

    public SearchProductsUseCaseImpl(ElasticsearchSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SearchDocument> execute(SearchQuery query) {
        return repository.search(query);
    }
}
