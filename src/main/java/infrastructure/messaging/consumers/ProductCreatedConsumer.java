package infrastructure.messaging.consumers;

import application.events.ProductCreatedEvent;
import application.usecases.IndexProductUseCase;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Profile("!webtest")
@RequiredArgsConstructor
public class ProductCreatedConsumer {

    private final IndexProductUseCase indexProductUseCase;

    @RabbitListener(queues = "search.product.created")
    public void consume(ProductCreatedEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag)
                        throws IOException {
        try {
            log.info("Received ProductCreatedEvent to index product: {}", event.data().productId());
            indexProductUseCase.execute(event.data());
            channel.basicAck(tag, false);
            log.info("Successfully indexed and acknowledged product: {}", event.data().productId());
        } catch (Exception e) {
            log.error("Error indexing product {}: {}", event.data() != null ? event.data().productId() : "unknown", e.getMessage(), e);
            channel.basicNack(tag, false, false); // va a DLQ
        }
    }
}
