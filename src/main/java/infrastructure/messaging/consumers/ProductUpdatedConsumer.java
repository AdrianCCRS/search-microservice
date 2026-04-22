package infrastructure.messaging.consumers;

import application.events.ProductCreatedEvent;
import application.usecases.IndexProductUseCase;
import application.usecases.InvalidateCacheUseCase;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUpdatedConsumer {

    private final IndexProductUseCase indexProductUseCase;
    private final InvalidateCacheUseCase invalidateCacheUseCase;

    @RabbitListener(queues = "search.product.updated")
    public void consume(ProductCreatedEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag)
                        throws IOException {
        try {
            log.info("Received ProductUpdatedEvent for product: {}", event.data().productId());
            // 1. Reindexar el producto modificado
            indexProductUseCase.execute(event.data());
            
            // 2. Invalidar caché
            invalidateCacheUseCase.execute(event.data().productId());
            
            channel.basicAck(tag, false);
            log.info("Successfully updated and invalidated cache for product: {}", event.data().productId());
            
        } catch (Exception e) {
            log.error("Error updating product {}: {}", event.data() != null ? event.data().productId() : "unknown", e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }
}
