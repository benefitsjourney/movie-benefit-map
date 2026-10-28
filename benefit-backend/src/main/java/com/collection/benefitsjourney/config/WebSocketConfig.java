package com.collection.benefitsjourney.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    public WebSocketConfig(
        WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }


    // 設定 STOMP 訊息路由
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 後端推送給前端的訊息使用 /topic 開頭
        registry.enableSimpleBroker("/topic");

        // 前端傳給後端的訊息使用 /app 開頭
        registry.setApplicationDestinationPrefixes("/app");
    }

    // 設定 WebSocket 連線端點
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://benefitsjourney.github.io",
                        "https://movie-benefit-map.onrender.com",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173");
    }
}