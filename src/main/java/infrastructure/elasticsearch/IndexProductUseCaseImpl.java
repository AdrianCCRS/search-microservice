package infrastructure.elasticsearch;

import application.events.ProductData;
import application.usecases.IndexProductUseCase;
import domain.entities.SearchDocument;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndexProductUseCaseImpl implements IndexProductUseCase {

    private final ElasticsearchSearchRepository elasticsearchSearchRepository;
    private final Timer indexTimer;

    public IndexProductUseCaseImpl(
            ElasticsearchSearchRepository elasticsearchSearchRepository,
            MeterRegistry meterRegistry) {
        this.elasticsearchSearchRepository = elasticsearchSearchRepository;
        this.indexTimer = Timer.builder("search.elasticsearch.index.duration")
                .description("Time taken to index a product document in Elasticsearch")
                .tag("operation", "index")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

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

        indexTimer.record(() -> {
            elasticsearchSearchRepository.indexDocument(document);
            log.info("Indexed product {} in Elasticsearch", data.productId());
        });
    }
}
