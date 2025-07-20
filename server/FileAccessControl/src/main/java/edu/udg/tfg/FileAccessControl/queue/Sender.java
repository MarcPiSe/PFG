package edu.udg.tfg.FileAccessControl.queue;

import edu.udg.tfg.FileAccessControl.config.RabbitConfig;
import edu.udg.tfg.FileAccessControl.queue.messages.DeleteElement;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${spring.application.name}")
    String serviceName;

    public void confirmDelete(DeleteElement deleteElement) {
        Map<String, Object> payload = Map.of(
                "userId", deleteElement.userId(),
                "elementId", deleteElement.elementId(),
                "service", serviceName
        );
        rabbitTemplate.convertAndSend(RabbitConfig.CONFIRM_DELETE, payload);
    }

    public void sendDeletionConfirmation(UUID userId) { 
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "service", "FileAccessControl"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }
}
