package infrastructure.redis;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisSearchCacheRepository {

    private final StringRedisTemplate redisTemplate;

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

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis cache key is required");
        }
    }
}
