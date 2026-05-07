package infrastructure.redis;

import application.events.ProductData;
import application.usecases.InvalidateCacheUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInvalidateCacheUseCase implements InvalidateCacheUseCase {

    private static final String PRODUCT_UPDATED_EVENT = "ProductUpdated";
    private static final String PRODUCT_CREATED_EVENT = "ProductCreated";

    private final RedisSearchCacheRepository cacheRepository;
    private final SearchCacheKeyFactory keyFactory;

    @Value("${search.cache.redis.query-key-pattern:search:query:*}")
    private String queryKeyPattern;

    @Value("${search.cache.redis.suggest-key-pattern:search:suggest:*}")
    private String suggestKeyPattern;

    @Override
    public void invalidateProductUpdated(ProductData data) {
        validateData(data);

        long deleted = cacheRepository.deleteByPattern(queryKeyPattern);
        deleted += cacheRepository.deleteByPattern(suggestPatternForProductName(data.name()));

        log.info("Invalidadas {} claves de caché por evento {}", deleted, PRODUCT_UPDATED_EVENT);
    }

    @Override
    public void invalidateProductCreated(ProductData data) {
        validateData(data);

        long deleted = cacheRepository.deleteByPattern(suggestKeyPattern);

        log.info("Invalidadas {} claves de caché por evento {}", deleted, PRODUCT_CREATED_EVENT);
    }

    private String suggestPatternForProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            return suggestKeyPattern;
        }

        String suggestPrefix = suggestKeyPattern.endsWith("*")
                ? suggestKeyPattern.substring(0, suggestKeyPattern.length() - 1)
                : suggestKeyPattern;
        return suggestPrefix + keyFactory.normalize(productName) + "*";
    }

    private void validateData(ProductData data) {
        if (data == null) {
            throw new IllegalArgumentException("Product data is required to invalidate Redis cache");
        }
    }
}
