package edu.udg.tfg.FileManagement.queue;

import edu.udg.tfg.FileManagement.config.RabbitConfig;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderStructure;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.queue.messages.DeleteAccess;
import edu.udg.tfg.FileManagement.queue.messages.DeleteElement;
import edu.udg.tfg.FileManagement.services.ElementService;
import edu.udg.tfg.FileManagement.services.FileAccessService;
import edu.udg.tfg.FileManagement.services.FileService;
import edu.udg.tfg.FileManagement.services.FolderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.Map;

@Component
public class Receiver {

    @Autowired
    ElementService elementService;

    @Autowired
    FileService fileService;

    @Autowired
    FolderService folderService;

    @Autowired
    FileAccessService fileAccessService;

    @Autowired
    private Sender sender;

    @RabbitListener(queues = RabbitConfig.DELETE_USER_FM, messageConverter = "jackson2JsonMessageConverter")
    public void receiveMessage(final String userId) {
        fileService.deleteByUserId(UUID.fromString(userId));
        folderService.deleteByUserId(UUID.fromString(userId));
        sender.sendDeletionConfirmation(UUID.fromString(userId));
    }

    @RabbitListener(queues = RabbitConfig.DELETE_ELEMENT, messageConverter = "jackson2JsonMessageConverter")
    public void deleteElement(final Map<String, Object> payload) {
        UUID elementId = UUID.fromString((String) payload.get("elementId"));
        boolean isFolder = elementService.isFolder(elementId);
        try {
            if (isFolder) {
                folderService.removeFolder(elementId);
            } else {
                fileService.deleteFile(elementId);
            }
            DeleteElement deleteElement = new DeleteElement((String) payload.get("userId"), (String) payload.get("elementId"));
            sender.confirmDelete(deleteElement);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}