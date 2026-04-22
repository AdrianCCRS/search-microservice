package infrastructure.redis;

import application.usecases.InvalidateCacheUseCase;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInvalidateCacheUseCase implements InvalidateCacheUseCase {

    private final StringRedisTemplate redisTemplate;

    @Value("${search.cache.redis.product-key-prefix:search:product:}")
    private String productKeyPrefix;

    @Value("${search.cache.redis.query-key-pattern:search:query:*}")
    private String queryKeyPattern;

    @Value("${search.cache.redis.scan-count:1000}")
    private long scanCount;

    @Override
    public void execute(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId is required to invalidate Redis cache");
        }

        Set<String> keysToDelete = new HashSet<>();
        keysToDelete.add(productKeyPrefix + productId);
        keysToDelete.addAll(scanQueryCacheKeys());

        Long deletedKeys = redisTemplate.delete(keysToDelete);
        log.info("Invalidated {} Redis cache keys for product {}", deletedKeys != null ? deletedKeys : 0, productId);
    }

    private Set<String> scanQueryCacheKeys() {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(queryKeyPattern)
                .count(scanCount)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        return keys;
    }
}
