package edu.udg.tfg.UserAuthentication.queue;

import edu.udg.tfg.UserAuthentication.config.RabbitConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.Map;

@Component
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    public void sendDeletionConfirmation(UUID userId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "service", "UserAuthentication"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }
}
