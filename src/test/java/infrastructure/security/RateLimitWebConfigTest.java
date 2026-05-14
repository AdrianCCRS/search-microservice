package infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitWebConfigTest {

    @Test
    void addInterceptors_registersRateLimitInterceptorForSearchPaths() {
        RateLimitInterceptor interceptor = mock(RateLimitInterceptor.class);

        RateLimitWebConfig config = new RateLimitWebConfig();
        // Inject mock interceptor via reflection (field is @Autowired)
        org.springframework.test.util.ReflectionTestUtils.setField(config, "rateLimitInterceptor", interceptor);

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);

        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        when(registration.addPathPatterns(any(String.class), any(String.class))).thenReturn(registration);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
        verify(registration).addPathPatterns("/api/search", "/api/search/suggest");
    }
}
