package com.example.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broadcasts to clients will be on destinations starting with /topic
        config.enableSimpleBroker("/topic");
        
        // Messages from clients (if they send STOMP messages directly) start with /app
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint URL clients use to establish the WebSocket connection
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for our test setup
                .withSockJS(); // Fallback for browsers that don't support raw WebSockets
    }
}
