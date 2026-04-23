package infrastructure.elasticsearch;

import application.events.ProductData;
import application.usecases.IndexProductUseCase;
import domain.entities.SearchDocument;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class IndexProductUseCaseImpl implements IndexProductUseCase {

    private final ElasticsearchSearchRepository searchRepository;

    public IndexProductUseCaseImpl(ElasticsearchSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Override
    public void execute(ProductData data) {
        SearchDocument document = new SearchDocument(
            data.productId(),
            data.name(),
            data.description(),
            data.category(),
            data.price() != null ? BigDecimal.valueOf(data.price()) : null,
            data.rating(),
            data.available(),
            data.brand()
        );
        searchRepository.indexDocument(document);
    }
}
