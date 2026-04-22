package application.usecases;

import application.events.ProductData;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DummyIndexProductUseCase implements IndexProductUseCase {
    @Override
    public void execute(ProductData data) {
        log.info("Dummy: Indexing product {} to Elasticsearch/Mongo", data.productId());
    }
}
