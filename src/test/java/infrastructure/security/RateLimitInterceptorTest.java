package infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import presentation.advice.RateLimitExceededException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(5);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    }

    @Test
    void allowsRequestsUnderLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, null),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void blocksRequestsOverLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, null),
                    "Request " + (i + 1) + " should be allowed");
        }

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, null));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void usesXForwardedForHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        assertTrue(interceptor.preHandle(request, response, null));

        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.3");
        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void tracksDifferentClientsSeparately() {
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        when(request2.getRemoteAddr()).thenReturn("192.168.1.2");

        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, null));
        }

        assertThrows(RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, null));

        assertTrue(interceptor.preHandle(request2, response2, null),
                "Different client should not be blocked");
    }
}
