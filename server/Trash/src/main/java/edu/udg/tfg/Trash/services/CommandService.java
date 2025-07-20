package edu.udg.tfg.Trash.services;

import edu.udg.tfg.Trash.queue.Sender;
import edu.udg.tfg.Trash.queue.messages.CommandRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class CommandService {
    @Autowired
    private Sender sender;

    public void sendTrashClean(String userId, String elementId) {
        sender.sendCommand(
                new CommandRabbit("trash-clean", userId, elementId, "", "", "", "", "", ""
                ));
    }


}
