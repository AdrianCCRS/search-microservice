package infrastructure.elasticsearch;

import application.usecases.SearchProductsUseCase;
import domain.entities.SearchDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchProductsUseCaseImpl implements SearchProductsUseCase {

    private final ElasticsearchSearchRepository elasticsearchSearchRepository;

    @Override
    public List<SearchDocument> search(String q) {
        return elasticsearchSearchRepository.search(q);
    }
}