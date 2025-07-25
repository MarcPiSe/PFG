package edu.udg.tfg.SyncService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        RawWebSocketHandler handler = rawWebSocketHandler();
        
        registry.addHandler(handler, "/websocket")
                .setAllowedOrigins("*")
                .addInterceptors(new UserIdHandshakeInterceptor());
                
        registry.addHandler(handler, "/websocket/web")
                .setAllowedOrigins("*")
                .addInterceptors(new UserIdHandshakeInterceptor());

        System.out.println("WebSocket handlers registrados correctamente");
    }
    
    @Bean
    public RawWebSocketHandler rawWebSocketHandler() {
        return new RawWebSocketHandler();
    }
} 