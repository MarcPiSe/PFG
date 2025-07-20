package edu.udg.tfg.SyncService.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.udg.tfg.SyncService.entities.SnapshotEntity;
import edu.udg.tfg.SyncService.entities.SnapshotElementEntity;
import edu.udg.tfg.SyncService.services.SnapshotService;
import edu.udg.tfg.SyncService.services.WebSocketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class RawWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SnapshotService snapshotService;
    
    private final Map<String, Map<String, WebSocketSession>> snapshotSessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, WebSocketSession>> webSessions = new ConcurrentHashMap<>();
    
    public enum ConnectionType {
        GENERAL, WEB
    }
    
    public RawWebSocketHandler() {
        
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String connectionId = (String) session.getAttributes().get("connectionId");
        
        if (userId != null && connectionId != null) {
            ConnectionType connectionType = detectConnectionType(session);
            session.getAttributes().put("connectionType", connectionType.name());
            
            if (connectionType == ConnectionType.WEB) {
                webSessions.putIfAbsent(userId, new ConcurrentHashMap<>());
                webSessions.get(userId).put(connectionId, session);
            } else {
                snapshotSessions.putIfAbsent(userId, new ConcurrentHashMap<>());
                snapshotSessions.get(userId).put(connectionId, session);
                SnapshotEntity snapshot = snapshotService.getSnapshotByUserId(UUID.fromString(userId));
                SnapshotElementEntity root = snapshot.getElements().stream().filter(element -> element.getParent() == null).findFirst().orElse(null);
                    sendMessage(session, WebSocketService.createMessage("CONNECTED", "actual_snapshot", WebSocketService.convertElementToMap(root)));
            }
            
        } else {
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String connectionId = (String) session.getAttributes().get("connectionId");
        ConnectionType connectionType = ConnectionType.valueOf((String) session.getAttributes().get("connectionType"));
        
        if (userId != null && connectionId != null) {
            if (connectionType == ConnectionType.WEB) {
                Map<String, WebSocketSession> userSessions = webSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(connectionId);
                    if (userSessions.isEmpty()) {
                        webSessions.remove(userId);
                    }
                }
            } else {
                Map<String, WebSocketSession> userSessions = snapshotSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(connectionId);
                    if (userSessions.isEmpty()) {
                        snapshotSessions.remove(userId);
                    }
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String connectionId = (String) session.getAttributes().get("connectionId");
        ConnectionType connectionType = ConnectionType.valueOf((String) session.getAttributes().get("connectionType"));
        
        if (userId != null && connectionId != null) {
            if (connectionType == ConnectionType.WEB) {
                Map<String, WebSocketSession> userSessions = webSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(connectionId);
                    if (userSessions.isEmpty()) {
                        webSessions.remove(userId);
                    }
                }
            } else {
                Map<String, WebSocketSession> userSessions = snapshotSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(connectionId);
                    if (userSessions.isEmpty()) {
                        snapshotSessions.remove(userId);
                    }
                }
            }
        }
    }

    private ConnectionType detectConnectionType(WebSocketSession session) {
        String uri = session.getUri().getPath();
        ConnectionType connectionType = uri.contains("/websocket/web") ? ConnectionType.WEB : ConnectionType.GENERAL;
        return connectionType;
    }

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    private String createSimpleMessage(String type, String message, Map<String, Object> data) {
        try {
            Map<String, Object> messageMap = Map.of(
                "type", type,
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "data", data != null ? data : Map.of()
            );
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"message\":\"Error creating message\",\"timestamp\":" + System.currentTimeMillis() + "}";
        }
    }
    
    public void sendMessageToUser(String userId, String message, String connectionId) throws IOException {
        sendSnapshot(userId, message, connectionId);
        sendWebCommand(userId, message, connectionId);
    }

    public void sendSnapshot(String userId, String message, String connectionId) throws IOException {
        Map<String, WebSocketSession> sessions = snapshotSessions.get(userId);
        if (sessions != null) {
            for(String connection : sessions.keySet()) {
                if(!connection.equals(connectionId)) {
                    if(sessions.get(connection) != null && sessions.get(connection).isOpen()) {
                        sendMessage(sessions.get(connection), message);
                    }
                }
            }
        }
    }

    public void sendWebCommand(String userId, String message, String connectionId) throws IOException {
        Map<String, WebSocketSession> sessions = webSessions.get(userId);
        if (sessions != null) {
            for(String connection : sessions.keySet()) {
                if(!connection.equals(connectionId)) {
                    if(sessions.get(connection) != null && sessions.get(connection).isOpen()) {
                        sendMessage(sessions.get(connection), message);
                    }
                }
            }
        }
    }

    public void broadcastToSnapshots(String message) throws IOException {
        for (Map<String, WebSocketSession> userSessions : snapshotSessions.values()) {
            for (WebSocketSession session : userSessions.values()) {
                if (session != null && session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    public void broadcastToWeb(String message) throws IOException {
        for (Map<String, WebSocketSession> userSessions : webSessions.values()) {
            for (WebSocketSession session : userSessions.values()) {
                if (session != null && session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    public boolean isUserConnected(String userId) {
        return snapshotSessions.containsKey(userId) || webSessions.containsKey(userId);
    }
} 