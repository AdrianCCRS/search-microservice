package infrastructure.messaging.consumers;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DeadLetterConsumerTest {

    private final DeadLetterConsumer consumer = new DeadLetterConsumer();

    @Test
    void consumeDeadLetterDoesNotThrow() {
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        when(message.getBody()).thenReturn("dead-letter-payload".getBytes());
        when(message.getMessageProperties()).thenReturn(properties);

        assertDoesNotThrow(() -> consumer.consumeDeadLetter(message));
    }

    @Test
    void consumeDeadLetterHandlesEmptyBody() {
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        when(message.getBody()).thenReturn(new byte[0]);
        when(message.getMessageProperties()).thenReturn(properties);

        assertDoesNotThrow(() -> consumer.consumeDeadLetter(message));
    }
}
