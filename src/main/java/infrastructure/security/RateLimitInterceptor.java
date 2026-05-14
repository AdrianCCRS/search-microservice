package infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import presentation.advice.RateLimitExceededException;

@Component
@ConditionalOnProperty(name = "search.security.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    private final int requestsPerMinute;
    private final Map<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            @Value("${search.security.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientId = resolveClientId(request);
        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(clientId, k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= requestsPerMinute) {
                throw new RateLimitExceededException("Demasiadas solicitudes. Límite: "
                        + requestsPerMinute + " por minuto. Reintente más tarde.");
            }

            timestamps.addLast(now);
        }

        return true;
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
