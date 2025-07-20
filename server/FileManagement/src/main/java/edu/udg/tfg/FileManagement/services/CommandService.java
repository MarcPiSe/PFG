package edu.udg.tfg.FileManagement.services;

import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.queue.Sender;
import edu.udg.tfg.FileManagement.queue.messages.CommandRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class CommandService {
    @Autowired
    private Sender sender;

    private CommandRabbit commandRabbit(String action, String userId, String connectionId, String path, String elementId, String hash, String name, String type, String parent) {
        return new CommandRabbit(action, userId, elementId, hash, path, name, type, connectionId, parent);
    }

    private String getPath(FolderEntity parent) {
        if(parent.getParent() == null) return "";
        return getPath(parent.getParent()) + "/" + parent.getName();
    }

    public void sendCreate(UUID elementId, String connectionId, String userId, FolderEntity parent, String hash, String name, String type) {
        String path = "/";
        String parentId = "";
        if(parent != null) {
            path = getPath(parent) + "/" + name;
            parentId = parent.getElementId().toString();
        }
        sender.sendCommand(
                new CommandRabbit("create", userId, elementId.toString(), hash, path, name, type, connectionId, parentId
                ));
    }

    public void sendUpdate(UUID elementId, String connectionId, String userId, FolderEntity parent, String hash, String name, String type) {
        String path = "/";
        String parentId = "";
        if(parent != null) {
            path = getPath(parent) + "/" + name;
            parentId = parent.getElementId().toString();
        }
        sender.sendCommand(
                new CommandRabbit("modify", userId, elementId.toString(), hash, path, name, type, connectionId, parentId
                ));
    }

    public void sendDelete(UUID elementId, String connectionId, String userId, FolderEntity parent, String hash, String name, String type) {
        String path = "/";
        String parentId = "";
        if(parent != null) {
            path = getPath(parent) + "/" + name;
            parentId = parent.getElementId().toString();
        }
        sender.sendCommand(
                new CommandRabbit("delete", userId, elementId.toString(), hash, path, name, type, connectionId, parentId
                ));
    }
}
