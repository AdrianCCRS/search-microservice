package infrastructure.elasticsearch;

import application.events.ProductData;
import application.usecases.IndexProductUseCase;
import domain.entities.SearchDocument;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexProductUseCaseImpl implements IndexProductUseCase {

    private final ElasticsearchSearchRepository elasticsearchSearchRepository;

    @Override
    public void execute(ProductData data) {
        if (data == null || data.productId() == null || data.productId().isBlank()) {
            throw new IllegalArgumentException("Product data with productId is required to index search document");
        }

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

        elasticsearchSearchRepository.indexDocument(document);
        log.info("Indexed product {} in Elasticsearch", data.productId());
    }
}
