package edu.udg.tfg.SyncService.services;

import edu.udg.tfg.SyncService.config.RawWebSocketHandler;
import edu.udg.tfg.SyncService.entities.SnapshotEntity;
import edu.udg.tfg.SyncService.entities.SnapshotElementEntity;
import edu.udg.tfg.SyncService.feignClients.FileManagementClient;
import edu.udg.tfg.SyncService.feignClients.fileShare.FileShareClient;
import edu.udg.tfg.SyncService.feignClients.fileShare.SharedInfo;
import edu.udg.tfg.SyncService.feignClients.UserManagementClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class WebSocketService {

    @Autowired
    private RawWebSocketHandler webSocketHandler;

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private FileManagementClient fileManagementClient;

    @Autowired
    private FileShareClient fileShareClient;

    @Transactional
    public void sendSnapshot(SnapshotEntity snapshot, String userId, String connectionId) {
        try {
            SnapshotElementEntity root = snapshot.getElements().stream().filter(element -> element.getParent() == null).findFirst().orElse(null);
            String snapshotMessage = createMessage("SNAPSHOT", "snapshot_update", convertElementToMap(root));
            
            webSocketHandler.sendSnapshot(userId, snapshotMessage, connectionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSection(String action) {
        if(action.equals("shared-element")) {
            return "shared";
        } else if(action.equals("unshared-element")) {
            return "shared";
        } else if(action.equals("trash-clean")) {
            return "trash";
        } else {
            return "root";
        }
    }

    public void sendWebCommand(String action, String userId, String connectionId, String elementId, String parentId) {
        try {
            if(parentId == null || parentId.isEmpty()) {
                parentId = fileManagementClient.getParentId(UUID.fromString(elementId)).toString();
            }

            String message = createMessage("COMMAND", "updated_tree", Map.of("elementId", elementId, "parentId", parentId, "section", getSection(action)));
            
            webSocketHandler.sendWebCommand(userId, message, connectionId);

            List<SharedInfo> users = fileShareClient.getUsersShared(UUID.fromString(elementId));

            message = createMessage("COMMAND", "updated_tree", Map.of("elementId", elementId, "parentId", parentId, "section", "shared"));
            for(SharedInfo user : users) {
                UUID id = userManagementClient.getUserId(user.getUsername());
                webSocketHandler.sendWebCommand(id.toString(), message, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifySnapshotChange(String userId, String changeType, Object changeData, String connectionId) {
        try {
            String message = createMessage("SNAPSHOT_CHANGE", changeType, changeData);
            
            webSocketHandler.sendMessageToUser(userId, message, connectionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUserConnected(String userId) {
        return webSocketHandler.isUserConnected(userId);
    }

    public static Map<String, Object> convertElementToMap(SnapshotElementEntity element) {
        if (element == null) {
            return Map.of();
        }
        
        Map<String, Object> elementMap = new java.util.LinkedHashMap<>();
        elementMap.put("type", element.getType());
        elementMap.put("hash", element.getHash());
        elementMap.put("path", element.getPath());
        elementMap.put("id", element.getElementId().toString());

        if (element.getParent() != null) {
            elementMap.put("parent_id", element.getParent().getElementId().toString());
        }

        if (element.getContent() != null && !element.getContent().isEmpty()) {
            java.util.Map<String, Object> contentMap = element.getContent().stream()
                .collect(java.util.stream.Collectors.toMap(
                    SnapshotElementEntity::getName,
                    WebSocketService::convertElementToMap,
                    (a, b) -> a,
                    java.util.LinkedHashMap::new
                ));
            elementMap.put("content", contentMap);
        } else {
            elementMap.put("content", java.util.Map.of());
        }
        
        return elementMap;
    }

    public static String createMessage(String type, String message, Object data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> messageMap = Map.of(
            "type", type,
            "message", message,
            "timestamp", System.currentTimeMillis(),
            "data", data != null ? data : Map.of()
        );
        
        return objectMapper.writeValueAsString(messageMap);
    }
} 