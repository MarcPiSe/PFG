package edu.udg.tfg.Trash.queue;

import edu.udg.tfg.Trash.config.RabbitConfig;
import edu.udg.tfg.Trash.queue.messages.CommandRabbit;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


@Component
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    public void removeAccess(UUID userId, UUID elementId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "elementId", elementId.toString()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_ACCESS_QUEUE, payload);
    }

    public void removeSharing(UUID userId, UUID elementId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "elementId", elementId.toString()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_SHARING, payload);
    }

    public void removeManagement(UUID userId, UUID elementId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "elementId", elementId.toString()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_ELEMENT, payload);
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
                "service", "Trash"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }
}
