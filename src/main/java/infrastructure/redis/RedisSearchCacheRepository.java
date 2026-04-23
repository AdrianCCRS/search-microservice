package infrastructure.redis;

import domain.repositories.SearchCacheRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSearchCacheRepository implements SearchCacheRepository {

    private final StringRedisTemplate redisTemplate;

    public RedisSearchCacheRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void deleteProduct(String productId) {
        String cacheKey = "product:" + productId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        
        if (Boolean.TRUE.equals(deleted)) {
            System.out.println("Cache invalidado para producto: " + productId);
        } else {
            System.out.println("No se encontro cache para invalidar: " + productId);
        }
    }
}
