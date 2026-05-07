package infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
public class RedisSearchCacheRepository {

    private static final long SUGGEST_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSearchCacheRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<String>> getSuggestions(String query) {
        String key = buildKey(query);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            List<String> result = objectMapper.readValue(value, new TypeReference<List<String>>() {});
            return Optional.of(result);
        } catch (Exception e) {
            System.err.println("Error deserializando sugerencias de Redis: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void setSuggestions(String query, List<String> suggestions) {
        String key = buildKey(query);
        try {
            String value = objectMapper.writeValueAsString(suggestions);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(SUGGEST_TTL_SECONDS));
        } catch (Exception e) {
            System.err.println("Error serializando sugerencias para Redis: " + e.getMessage());
        }
    }

    private String buildKey(String query) {
        return "suggest:" + query.toLowerCase();
    }
}
