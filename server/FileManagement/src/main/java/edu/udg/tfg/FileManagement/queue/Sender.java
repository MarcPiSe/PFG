package edu.udg.tfg.FileManagement.queue;

import edu.udg.tfg.FileManagement.config.RabbitConfig;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.queue.messages.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Map;

@Service
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${spring.application.name}")
    String serviceName;

    public void deleteAccess(UUID elementId, UUID userId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "elementId", elementId.toString()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_ACCESS_QUEUE, payload);
    }

    public void confirmDelete(DeleteElement deleteElement) {
        Map<String, Object> payload = Map.of(
                "userId", deleteElement.userId(),
                "elementId", deleteElement.elementId(),
                "service", serviceName
        );
        rabbitTemplate.convertAndSend(RabbitConfig.CONFIRM_DELETE, payload);
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

    public void sendDeletionConfirmation(UUID userId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "service", "FileManagement"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }
}
