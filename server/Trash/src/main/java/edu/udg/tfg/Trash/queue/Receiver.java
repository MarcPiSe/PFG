package edu.udg.tfg.Trash.queue;

import edu.udg.tfg.Trash.queue.messages.DeletionConfirmation;
import edu.udg.tfg.Trash.services.TrashService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.udg.tfg.Trash.config.RabbitConfig;

import java.util.Map;
import java.util.UUID;

@Component
public class Receiver {

    @Autowired
    private TrashService trashService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.CONFIRM_DELETE, messageConverter = "jackson2JsonMessageConverter")
    public void confirmDeleteAccess(Map<String, Object> payload) {
        UUID elementId = UUID.fromString((String) payload.get("elementId"));
        UUID userId = UUID.fromString((String) payload.get("userId"));
        String service = (String) payload.get("service");
        trashService.confirm(elementId, userId, service);
    }

    @RabbitListener(queues = RabbitConfig.DELETE_USER_TR, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        trashService.deleteByUserId(UUID.fromString(userId));
        sender.sendDeletionConfirmation(UUID.fromString(userId));
    }
}