package infrastructure.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisSearchCacheRepository {

    private final StringRedisTemplate redisTemplate;

    @Value("${search.cache.redis.scan-count:1000}")
    private long scanCount;

    @Value("${search.cache.redis.unlink-batch-size:500}")
    private int unlinkBatchSize;

    public Optional<String> get(String key) {
        validateKey(key);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void set(String key, String value, Duration ttl) {
        validateKey(key);
        if (value == null) {
            throw new IllegalArgumentException("value is required to cache search data");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be greater than zero");
        }

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void delete(String key) {
        validateKey(key);
        redisTemplate.delete(key);
    }

    public long deleteByPattern(String pattern) {
        validateKey(pattern);

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(Math.max(scanCount, 1))
                .build();
        int batchSize = Math.max(unlinkBatchSize, 1);
        List<String> batch = new ArrayList<>(batchSize);
        long deleted = 0;

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= batchSize) {
                    deleted += unlink(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            deleted += unlink(batch);
        }

        return deleted;
    }

    private long unlink(List<String> keys) {
        Long deleted = redisTemplate.unlink(List.copyOf(keys));
        return deleted == null ? 0 : deleted;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis cache key is required");
        }
    }
}
