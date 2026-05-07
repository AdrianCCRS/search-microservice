package infrastructure.messaging.consumers;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductEventsConsumerIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldIndexProductWhenCreatedEventIsPublished() throws InterruptedException {

        Map<String, Object> event = Map.of(
                "id", "1",
                "name", "Laptop Gamer",
                "price", 1000
        );

        rabbitTemplate.convertAndSend(
                "catalog.events",
                "product.created",
                event
        );

        Thread.sleep(5000);

        String cached = redisTemplate.opsForValue().get("product:1");

        assertNotNull(cached);
    }

    @Test
    void shouldUpdateProductWhenUpdatedEventIsPublished() throws InterruptedException {

        Map<String, Object> event = Map.of(
                "id", "1",
                "price", 2000
        );

        rabbitTemplate.convertAndSend(
                "catalog.events",
                "product.updated",
                event
        );

        Thread.sleep(5000);

        String cached = redisTemplate.opsForValue().get("product:1");

        assertNotNull(cached);
    }

    @Test
    void shouldInvalidateCacheWhenProductIsUpdated() throws InterruptedException {

        redisTemplate.opsForValue().set("search:query:laptop", "cached-result");

        Map<String, Object> event = Map.of(
                "id", "1",
                "price", 3000
        );

        rabbitTemplate.convertAndSend(
                "catalog.events",
                "product.updated",
                event
        );

        Thread.sleep(5000);

        String cache = redisTemplate.opsForValue().get("search:query:laptop");

        assertNull(cache);
    }
}