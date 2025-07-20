package edu.udg.tfg.FileSharing.services;

import edu.udg.tfg.FileSharing.queue.Sender;
import edu.udg.tfg.FileSharing.queue.messages.CommandRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class CommandService {
    @Autowired
    private Sender sender;

    public void sendShared(UUID elementId, String connectionId, String userId, String parentId) {
        sender.sendCommand(
                new CommandRabbit("shared-element", userId, elementId.toString(), "", "", "", "", connectionId, parentId
                ));
    }

    public void sendUnshared(UUID elementId, String connectionId, String userId) {
        sender.sendCommand(
                new CommandRabbit("unshared-element", userId, elementId.toString(), "", "", "", "", connectionId, ""
                ));
    }


}
