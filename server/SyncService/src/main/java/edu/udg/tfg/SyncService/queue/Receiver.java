package edu.udg.tfg.SyncService.queue;

import com.netflix.discovery.converters.Auto;

import edu.udg.tfg.SyncService.config.RabbitConfig;
import edu.udg.tfg.SyncService.entities.SnapshotEntity;
import edu.udg.tfg.SyncService.queue.messages.CommandRabbit;
import edu.udg.tfg.SyncService.services.SnapshotService;
import edu.udg.tfg.SyncService.services.WebSocketService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class Receiver {

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private WebSocketService websocketService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.COMMAND_QUEUE, messageConverter = "jackson2JsonMessageConverter")
    public void processCommand(Map<String, Object> payload) {
        try {
            CommandRabbit command = new CommandRabbit(
                (String) payload.get("action"),
                (String) payload.get("userId"),
                (String) payload.get("elementId"),
                (String) payload.get("hash"),
                (String) payload.get("path"),
                (String) payload.get("name"),
                (String) payload.get("type"),
                (String) payload.get("connectionId"),
                (String) payload.get("parentId")
            );
            if(!command.action().equals("unshared-element") && !command.action().equals("shared-element")) {
                SnapshotEntity snapshot = snapshotService.processCommand(command);
                websocketService.sendSnapshot(snapshot, command.userId(), command.connectionId());
            }
            websocketService.sendWebCommand(command.action(), command.userId(), command.connectionId(), command.elementId(), command.parentId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.DELETE_USER_SS, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        snapshotService.deleteByUserId(UUID.fromString(userId));
        sender.sendDeletionConfirmation(UUID.fromString(userId));
    }
}
