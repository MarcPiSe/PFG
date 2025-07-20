package edu.udg.tfg.UserManagement.queue;

import edu.udg.tfg.UserManagement.config.RabbitConfig;
import edu.udg.tfg.UserManagement.services.UserDeletionService;
import edu.udg.tfg.UserManagement.queue.messages.DeletionConfirmation;
import edu.udg.tfg.UserManagement.services.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class Receiver {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDeletionService userDeletionService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.DELETE_USER_UM, messageConverter = "jackson2JsonMessageConverter")
    public void handleUserDeletion(final String userId) {
        if(userId == null) {
            return;
        }

        try {
            userService.deleteUserLocal(UUID.fromString(userId)); 
            sender.sendDeletionConfirmation(UUID.fromString(userId));
        } catch (Exception e) {
        }
    }

    @RabbitListener(queues = RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, messageConverter = "jackson2JsonMessageConverter")
    public void receiveDeletionConfirmation(Map<String, Object> payload) {
        UUID userId = UUID.fromString((String) payload.get("userId"));
        String service = (String) payload.get("service");
        DeletionConfirmation conf = new DeletionConfirmation(userId, service);
        userDeletionService.processDeletionConfirmation(conf);
    }
}