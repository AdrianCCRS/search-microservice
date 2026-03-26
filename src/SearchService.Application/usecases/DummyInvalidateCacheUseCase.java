package application.usecases;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DummyInvalidateCacheUseCase implements InvalidateCacheUseCase {
    @Override
    public void execute(String productId) {
        log.info("Dummy: Invalidating cache for product {}", productId);
    }
}
