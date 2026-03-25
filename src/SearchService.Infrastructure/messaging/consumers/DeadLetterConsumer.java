package infrastructure.messaging.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "search.dead.letter")
    public void consumeDeadLetter(Message message) {
        log.error("Message moved to Dead Letter Queue: {}", new String(message.getBody()));
        log.error("Headers and details: {}", message.getMessageProperties().getHeaders());
        // Aqui no hacemos ack/nack si usamos auto-ack o lo procesamos para alerta externa
    }
}
