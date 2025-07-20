package edu.udg.tfg.SyncService.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_CONNECTION_ID_HEADER = "X-Connection-Id";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            String userId = getHeaderValue(request, X_USER_ID_HEADER);
            String connectionId = getHeaderValue(request, X_CONNECTION_ID_HEADER);
            
            if (userId != null && connectionId != null) {
                attributes.put("userId", userId);
                attributes.put("connectionId", connectionId);
                return true;
            } else {
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                             WebSocketHandler wsHandler, Exception exception) {
    }
    
    private String getHeaderValue(ServerHttpRequest request, String headerName) {
        List<String> headerValues = request.getHeaders().get(headerName);
        return (headerValues != null && !headerValues.isEmpty()) ? headerValues.get(0) : null;
    }
} 