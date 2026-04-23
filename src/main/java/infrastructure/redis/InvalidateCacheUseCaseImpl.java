package infrastructure.redis;

import application.usecases.InvalidateCacheUseCase;
import domain.repositories.SearchCacheRepository;
import org.springframework.stereotype.Service;

@Service
public class InvalidateCacheUseCaseImpl implements InvalidateCacheUseCase {

    private final SearchCacheRepository cacheRepository;

    public InvalidateCacheUseCaseImpl(SearchCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @Override
    public void execute(String productId) {
        cacheRepository.deleteProduct(productId);
    }
}
