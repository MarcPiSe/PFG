package edu.udg.tfg.SyncService.queue;

import edu.udg.tfg.SyncService.config.RabbitConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import edu.udg.tfg.SyncService.queue.messages.CommandRabbit;

import java.util.Map;
import java.util.UUID;

@Component
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${spring.application.name}")
    String serviceName;

    public void sendDeletionConfirmation(UUID userId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "service", "SyncService"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }
}
