package edu.udg.tfg.UserAuthentication.queue;

import edu.udg.tfg.UserAuthentication.config.RabbitConfig;
import edu.udg.tfg.UserAuthentication.services.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Component
public class Receiver {
    @Autowired
    private UserService userService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.DELETE_USER_UA, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        try {
            userService.deleteByUser(UUID.fromString(userId));
            
            sender.sendDeletionConfirmation(UUID.fromString(userId));
        } catch (Exception e) {
            System.err.println("Error al eliminar usuario " + userId + " en UserAuthentication: " + e.getMessage());
        }
    }
}