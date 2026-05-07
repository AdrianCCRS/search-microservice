package infrastructure.messaging.consumers;

import application.events.ProductCreatedEvent;
import application.events.ProductData;
import application.usecases.IndexProductUseCase;
import application.usecases.InvalidateCacheUseCase;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.Mockito.*;

class ProductCreatedConsumerTest {

    @Mock
    private IndexProductUseCase indexProductUseCase;

    @Mock
    private InvalidateCacheUseCase invalidateCacheUseCase;

    @Mock
    private Channel channel;

    @InjectMocks
    private ProductCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConsumeSuccess() throws IOException {
        long deliveryTag = 1L;
        ProductData data = new ProductData("123", "Name", "Desc", "Cat", 10.0, 5.0, true, "Brand");
        ProductCreatedEvent event = new ProductCreatedEvent("ProductCreated", "1.0", "today", data);

        consumer.consume(event, channel, deliveryTag);

        verify(indexProductUseCase, times(1)).execute(data);
        verify(invalidateCacheUseCase, times(1)).invalidateProductCreated(data);
        verify(channel, times(1)).basicAck(deliveryTag, false);
    }

    @Test
    void testConsumeExceptionGoesToDLQ() throws IOException {
        long deliveryTag = 1L;
        ProductData data = new ProductData("123", "Name", "Desc", "Cat", 10.0, 5.0, true, "Brand");
        ProductCreatedEvent event = new ProductCreatedEvent("ProductCreated", "1.0", "today", data);

        doThrow(new RuntimeException("DB error")).when(indexProductUseCase).execute(data);

        consumer.consume(event, channel, deliveryTag);

        verify(indexProductUseCase, times(1)).execute(data);
        verify(invalidateCacheUseCase, never()).invalidateProductCreated(data);
        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(deliveryTag, false);
    }

    @Test
    void testConsumeInvalidationExceptionGoesToDLQ() throws IOException {
        long deliveryTag = 1L;
        ProductData data = new ProductData("123", "Name", "Desc", "Cat", 10.0, 5.0, true, "Brand");
        ProductCreatedEvent event = new ProductCreatedEvent("ProductCreated", "1.0", "today", data);

        doThrow(new RuntimeException("Redis error")).when(invalidateCacheUseCase).invalidateProductCreated(data);

        consumer.consume(event, channel, deliveryTag);

        verify(indexProductUseCase, times(1)).execute(data);
        verify(invalidateCacheUseCase, times(1)).invalidateProductCreated(data);
        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(deliveryTag, false);
    }
}
