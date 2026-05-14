package infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisSearchCacheRepositoryIT {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldStoreAndRetrieveValue() {
        redisTemplate.opsForValue().set("search:test", "value");

        String result = redisTemplate.opsForValue().get("search:test");

        assertEquals("value", result);
    }

    @Test
    void shouldExpireKeyAfterTTL() throws InterruptedException {
        redisTemplate.opsForValue().set("search:ttl", "temp", Duration.ofSeconds(1));

        Thread.sleep(1500);

        String result = redisTemplate.opsForValue().get("search:ttl");

        assertNull(result);
    }

    @Test
    void shouldDeleteKey() {
        redisTemplate.opsForValue().set("search:delete", "value");

        redisTemplate.delete("search:delete");

        String result = redisTemplate.opsForValue().get("search:delete");

        assertNull(result);
    }

    @Test
    void shouldDeleteKeysByPattern() {
        redisTemplate.opsForValue().set("search:query:1", "a");
        redisTemplate.opsForValue().set("search:query:2", "b");

        redisTemplate.delete(redisTemplate.keys("search:query:*"));

        assertTrue(redisTemplate.keys("search:query:*").isEmpty());
    }
}