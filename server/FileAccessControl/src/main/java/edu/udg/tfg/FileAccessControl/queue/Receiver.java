package edu.udg.tfg.FileAccessControl.queue;

import edu.udg.tfg.FileAccessControl.config.RabbitConfig;
import edu.udg.tfg.FileAccessControl.entities.AccessRule;
import edu.udg.tfg.FileAccessControl.entities.AccessType;
import edu.udg.tfg.FileAccessControl.queue.messages.DeleteElement;
import edu.udg.tfg.FileAccessControl.repositories.AccessRuleRepository;
import edu.udg.tfg.FileAccessControl.services.FileAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import java.util.Map;

@Component
public class Receiver {
    @Autowired
    FileAccessService fileAccessService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.DELETE_ACCESS_QUEUE, messageConverter = "jackson2JsonMessageConverter")
    public void handleElementPermanentlyDeleted(final Map<String, Object> payload) {
        String elementId = (String) payload.get("elementId");
        String userId = (String) payload.get("userId");
        fileAccessService.deleteAllAccessForElement(elementId);
        DeleteElement deleteElement = new DeleteElement(userId, elementId);
        sender.confirmDelete(deleteElement);
    }

    @RabbitListener(queues = RabbitConfig.DELETE_USER_FA, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        fileAccessService.deleteByUserId(UUID.fromString(userId));
        sender.sendDeletionConfirmation(UUID.fromString(userId));
    }
}