package edu.udg.tfg.FileSharing.queue;

import edu.udg.tfg.FileSharing.config.RabbitConfig;
import edu.udg.tfg.FileSharing.queue.messages.DeleteElement;
import edu.udg.tfg.FileSharing.queue.messages.CommandRabbit;
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
                "service", "FileSharing"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }

    public void sendCommand(CommandRabbit commandRabbit) {
        Map<String, String> payload = Map.of(
                "action", commandRabbit.action(),
                "userId", commandRabbit.userId(),
                "elementId", commandRabbit.elementId(),
                "hash", commandRabbit.hash(),
                "path", commandRabbit.path(),
                "name", commandRabbit.name(),
                "type", commandRabbit.type(),
                "connectionId", commandRabbit.connectionId(),
                "parentId", commandRabbit.parentId()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.COMMAND_QUEUE, payload);
    }
}
