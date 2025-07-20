package edu.udg.tfg.UserManagement.queue;

import edu.udg.tfg.UserManagement.config.RabbitConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


@Component
public class Sender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    public void sendDeletionConfirmation(UUID userId) {
        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "service", "UserManagement"
        );
        rabbitTemplate.convertAndSend(RabbitConfig.USER_DELETION_CONFIRMATION_QUEUE, payload);
    }



    public void sendDeleteCommandToFileManagement(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_FM, userId.toString());
    }

    public void sendDeleteCommandToFileSharing(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_FS, userId.toString());
    }

    public void sendDeleteCommandToFileAccessControl(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_FA, userId.toString());
    }

    public void sendDeleteCommandToUserManagement(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_UM, userId.toString());
    }

    public void sendDeleteCommandToUserAuthentication(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_UA, userId.toString());
    }

    public void sendDeleteCommandToSyncService(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_SS, userId.toString());
    }

    public void sendDeleteCommandToTrash(UUID userId) {
        rabbitTemplate.convertAndSend(RabbitConfig.DELETE_USER_TR, userId.toString());
    }
}
