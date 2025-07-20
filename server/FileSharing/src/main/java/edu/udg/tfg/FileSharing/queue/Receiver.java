package edu.udg.tfg.FileSharing.queue;

import edu.udg.tfg.FileSharing.queue.messages.DeleteElement;
import edu.udg.tfg.FileSharing.services.FileSharingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.udg.tfg.FileSharing.config.RabbitConfig;

import java.util.UUID;
import java.util.Map;

@Component
public class Receiver {

    @Autowired
    private FileSharingService fileSharingService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.DELETE_SHARING, messageConverter = "jackson2JsonMessageConverter")
    public void deleteElement(Map<String, Object> payload) {
        String elementId = (String) payload.get("elementId");
        String userId = (String) payload.get("userId");
        fileSharingService.handleElementDeleted(UUID.fromString(elementId));
        DeleteElement deleteElement = new DeleteElement(userId, elementId);
        sender.confirmDelete(deleteElement);
    }

    @RabbitListener(queues = RabbitConfig.DELETE_USER_FS, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        fileSharingService.deleteByUserId(UUID.fromString(userId));
        sender.sendDeletionConfirmation(UUID.fromString(userId));
    }
}